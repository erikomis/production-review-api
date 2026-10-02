package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.importer.ImportJobDTO;
import com.client.productionreview.dtos.importer.OpenFoodFactsProduct;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.integration.OpenFoodFactsClient;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.CatalogImportService;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.utils.SlugUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Importa categorias, subcategorias e produtos da API pública do Open Food Facts.
 * Idempotente pelo slug (e pelo código do produto); um job por vez, em segundo plano.
 */
@Slf4j
@Service
public class CatalogImportServiceImpl implements CatalogImportService {

    static final String SOURCE = "OPEN_FOOD_FACTS";
    static final String IMAGE_PREFIX = ProductImageServiceImpl.EXTERNAL_PREFIX + "off:";
    static final String STEP_SEPARATOR = " › ";
    static final int MAX_TEXT = 255;

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final DomainEventPublisher eventPublisher;
    private final CacheManager cacheManager;
    private final TransactionTemplate transactionTemplate;
    private final Executor executor;

    private final Map<String, ImportJobDTO> jobs = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private volatile String latestJobId;
    private volatile String runningJobId;

    @Autowired
    public CatalogImportServiceImpl(CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                    ProductRepository productRepository, ProductImageRepository productImageRepository,
                                    OpenFoodFactsClient openFoodFactsClient, DomainEventPublisher eventPublisher,
                                    CacheManager cacheManager, PlatformTransactionManager transactionManager) {
        this(categoryRepository, subCategoryRepository, productRepository, productImageRepository, openFoodFactsClient,
                eventPublisher, cacheManager, transactionManager, Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "catalog-import");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    public CatalogImportServiceImpl(CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                             ProductRepository productRepository, ProductImageRepository productImageRepository,
                             OpenFoodFactsClient openFoodFactsClient, DomainEventPublisher eventPublisher,
                             CacheManager cacheManager, PlatformTransactionManager transactionManager, Executor executor) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.openFoodFactsClient = openFoodFactsClient;
        this.eventPublisher = eventPublisher;
        this.cacheManager = cacheManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.executor = executor;
    }

    @PreDestroy
    void shutdown() {
        if (executor instanceof ExecutorService service) {
            service.shutdownNow();
        }
    }

    @Override
    public ImportJobDTO startOpenFoodFactsImport(int productsPerSubcategory, User startedBy) {
        ImportJobDTO job;
        synchronized (lock) {
            if (runningJobId != null) {
                throw new BusinessExcepion("Já existe uma importação em andamento");
            }
            job = ImportJobDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .source(SOURCE)
                    .status(ImportJobDTO.Status.RUNNING)
                    .totalSteps(OpenFoodFactsTaxonomy.totalSteps())
                    .startedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                    .startedBy(startedBy != null && startedBy.getName() != null ? startedBy.getName().trim() : null)
                    .build();
            jobs.put(job.getId(), job);
            latestJobId = job.getId();
            runningJobId = job.getId();
        }

        eventPublisher.publish(EventType.CATALOG_IMPORT_STARTED, job.getId(),
                nameOf(startedBy) + " iniciou a importação do Open Food Facts (" + productsPerSubcategory
                        + " produtos por subcategoria)", startedBy);

        ImportJobDTO snapshot = snapshot(job);
        try {
            executor.execute(() -> run(job, productsPerSubcategory, startedBy));
        } catch (RuntimeException e) {
            fail(job, "Não foi possível iniciar a importação: " + e.getMessage(), startedBy);
            throw e;
        }
        return snapshot;
    }

    @Override
    public ImportJobDTO getJob(String id) {
        ImportJobDTO job = jobs.get(id);
        if (job == null) {
            throw new NotFoundException("Import job not found");
        }
        return snapshot(job);
    }

    @Override
    public Optional<ImportJobDTO> getLatestJob() {
        String id = latestJobId;
        return id == null ? Optional.empty() : Optional.ofNullable(jobs.get(id)).map(this::snapshot);
    }

    void run(ImportJobDTO job, int productsPerSubcategory, User startedBy) {
        try {
            for (OpenFoodFactsTaxonomy.Cat cat : OpenFoodFactsTaxonomy.CATEGORIES) {
                Category category = findOrCreateCategory(job, cat);
                for (OpenFoodFactsTaxonomy.Sub sub : cat.subCategories()) {
                    String step = cat.name() + STEP_SEPARATOR + sub.name();
                    update(job, j -> j.setCurrentStep(step));
                    try {
                        SubCategory subCategory = findOrCreateSubCategory(job, sub, category);
                        importProducts(job, sub, subCategory, productsPerSubcategory);
                    } catch (Exception e) {
                        log.warn("Importação {}: {}", step, e.getMessage());
                        update(job, j -> j.getErrors().add(step + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())));
                    }
                    update(job, j -> j.setCompletedSteps(j.getCompletedSteps() + 1));
                }
            }
            finish(job, startedBy);
        } catch (Exception e) {
            log.error("Importação {} falhou", job.getId(), e);
            fail(job, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), startedBy);
        }
    }

    private Category findOrCreateCategory(ImportJobDTO job, OpenFoodFactsTaxonomy.Cat cat) {
        Optional<Category> existing = categoryRepository.findBySlug(cat.slug())
                .or(() -> categoryRepository.findByName(cat.name()));
        if (existing.isPresent()) {
            return existing.get();
        }
        Category category = Category.builder()
                .name(cat.name())
                .slug(cat.slug())
                .description(cat.name() + " — catálogo importado do Open Food Facts")
                .build();
        Category saved = categoryRepository.save(category);
        update(job, j -> j.setCategoriesCreated(j.getCategoriesCreated() + 1));
        return saved;
    }

    private SubCategory findOrCreateSubCategory(ImportJobDTO job, OpenFoodFactsTaxonomy.Sub sub, Category category) {
        Optional<SubCategory> existing = subCategoryRepository.findBySlug(sub.slug())
                .or(() -> subCategoryRepository.findByName(sub.name()));
        if (existing.isPresent()) {
            return existing.get();
        }
        SubCategory subCategory = SubCategory.builder()
                .name(sub.name())
                .slug(sub.slug())
                .description(sub.name() + " — catálogo importado do Open Food Facts")
                .categorieId(category.getId())
                .build();
        SubCategory saved = subCategoryRepository.save(subCategory);
        update(job, j -> j.setSubCategoriesCreated(j.getSubCategoriesCreated() + 1));
        return saved;
    }

    private void importProducts(ImportJobDTO job, OpenFoodFactsTaxonomy.Sub sub, SubCategory subCategory, int limit) {
        List<OpenFoodFactsProduct> products = openFoodFactsClient.searchProducts(sub.tag(), limit * 2);

        int taken = 0;
        for (OpenFoodFactsProduct offProduct : products) {
            if (taken >= limit) {
                break;
            }
            String name = productName(offProduct);
            String imageUrl = offProduct.getImageFrontUrl() == null ? null : offProduct.getImageFrontUrl().trim();
            String code = offProduct.getCode() == null ? null : offProduct.getCode().trim();
            if (name == null || code == null || code.isEmpty() || imageUrl == null || imageUrl.isEmpty()
                    || imageUrl.length() > MAX_TEXT) {
                update(job, j -> j.setProductsSkipped(j.getProductsSkipped() + 1));
                continue;
            }

            String slug = productSlug(name, code);
            String filename = IMAGE_PREFIX + code;
            // já importado antes (mesmo slug ou mesmo código): conta para o limite, mas não duplica
            if (productRepository.existsBySlug(slug) || productImageRepository.existsByFilename(filename)) {
                taken++;
                update(job, j -> j.setProductsSkipped(j.getProductsSkipped() + 1));
                continue;
            }

            transactionTemplate.executeWithoutResult(status -> {
                Product product = productRepository.save(Product.builder()
                        .name(name)
                        .slug(slug)
                        .description(description(offProduct, name))
                        .subCategorieId(subCategory.getId())
                        .build());

                ProductImage image = new ProductImage();
                image.setProductId(product.getId());
                image.setUrlImage(imageUrl);
                image.setType("image/jpeg");
                image.setFilename(filename);
                productImageRepository.save(image);
            });
            taken++;
            update(job, j -> {
                j.setProductsCreated(j.getProductsCreated() + 1);
                j.setImagesCreated(j.getImagesCreated() + 1);
            });
        }
    }

    static String productName(OpenFoodFactsProduct product) {
        String name = firstNonBlank(product.getProductNamePt(), product.getProductName());
        return name == null ? null : SlugUtils.truncate(name.replaceAll("\\s+", " "), MAX_TEXT);
    }

    static String productSlug(String name, String code) {
        String suffix = "-" + SlugUtils.slugify(code);
        String base = SlugUtils.slugify(name);
        int max = MAX_TEXT - suffix.length();
        if (base.length() > max) {
            base = base.substring(0, max).replaceAll("-+$", "");
        }
        return base + suffix;
    }

    /** generic_name_pt; senão "Marca: X · 500g · Nutri-Score A" (só as partes presentes). */
    static String description(OpenFoodFactsProduct product, String fallback) {
        String generic = firstNonBlank(product.getGenericNamePt());
        if (generic != null) {
            return SlugUtils.truncate(generic, MAX_TEXT);
        }
        List<String> parts = new ArrayList<>();
        String brands = firstNonBlank(product.getBrands());
        if (brands != null) {
            parts.add("Marca: " + brands);
        }
        String quantity = firstNonBlank(product.getQuantity());
        if (quantity != null) {
            parts.add(quantity);
        }
        String grade = firstNonBlank(product.getNutriscoreGrade());
        if (grade != null && grade.matches("(?i)[a-e]")) {
            parts.add("Nutri-Score " + grade.toUpperCase());
        }
        String description = parts.isEmpty() ? fallback : String.join(" · ", parts);
        return SlugUtils.truncate(description, MAX_TEXT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void finish(ImportJobDTO job, User startedBy) {
        update(job, j -> {
            j.setStatus(ImportJobDTO.Status.COMPLETED);
            j.setCurrentStep(null);
            j.setFinishedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        });
        clearCaches();
        release(job);
        ImportJobDTO done = snapshot(job);
        eventPublisher.publish(EventType.CATALOG_IMPORT_COMPLETED, job.getId(),
                "Importação concluída: " + done.getProductsCreated() + " produtos criados, "
                        + done.getProductsSkipped() + " ignorados, " + done.getErrors().size() + " erros", startedBy);
    }

    private void fail(ImportJobDTO job, String message, User startedBy) {
        update(job, j -> {
            j.setStatus(ImportJobDTO.Status.FAILED);
            j.getErrors().add(message);
            j.setFinishedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        });
        clearCaches();
        release(job);
        eventPublisher.publish(EventType.CATALOG_IMPORT_FAILED, job.getId(), "Importação falhou: " + message, startedBy);
    }

    private void release(ImportJobDTO job) {
        synchronized (lock) {
            if (job.getId().equals(runningJobId)) {
                runningJobId = null;
            }
        }
    }

    private void clearCaches() {
        if (cacheManager == null) {
            return;
        }
        for (String name : List.of("product", "category", "subCategory")) {
            try {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            } catch (Exception e) {
                log.warn("Falha ao limpar o cache {}: {}", name, e.getMessage());
            }
        }
    }

    private void update(ImportJobDTO job, java.util.function.Consumer<ImportJobDTO> change) {
        synchronized (job) {
            change.accept(job);
        }
    }

    private ImportJobDTO snapshot(ImportJobDTO job) {
        synchronized (job) {
            return job.toBuilder().errors(new ArrayList<>(job.getErrors())).build();
        }
    }

    private static String nameOf(User user) {
        return user != null && user.getName() != null ? user.getName().trim() : "Sistema";
    }
}
