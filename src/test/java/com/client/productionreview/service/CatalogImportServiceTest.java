package com.client.productionreview.service;

import com.client.productionreview.dtos.importer.ImportJobDTO;
import com.client.productionreview.dtos.importer.OpenFoodFactsProduct;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.integration.OpenFoodFactsClient;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.impl.CatalogImportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Importação contra o H2 de verdade (idempotência depende das consultas por slug);
 * o cliente do Open Food Facts é mockado, então nenhum teste chama a API real.
 */
@DataJpaTest
@ActiveProfiles(profiles = "test")
class CatalogImportServiceTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private OpenFoodFactsClient client;
    private DomainEventPublisher eventPublisher;
    private ConcurrentMapCacheManager cacheManager;
    private final User admin = User.builder().id(1L).name("Administrador").build();

    @BeforeEach
    void setUp() {
        client = mock(OpenFoodFactsClient.class);
        eventPublisher = mock(DomainEventPublisher.class);
        cacheManager = new ConcurrentMapCacheManager("product", "category", "subCategory");
        // para cada tag: 1 sem nome, 3 válidos e 1 sem imagem
        when(client.searchProducts(anyString(), anyInt())).thenAnswer(inv -> {
            String tag = inv.getArgument(0);
            return List.of(
                    OpenFoodFactsProduct.builder().code(tag + "-0").imageFrontUrl("https://img/0.jpg").build(),
                    OpenFoodFactsProduct.builder().code(tag + "-1").productNamePt("Aveia Em Flocos Nestlé " + tag)
                            .productName("Oat " + tag).brands("Nestlé").quantity("500g").nutriscoreGrade("a")
                            .imageFrontUrl("https://img/" + tag + "-1.jpg").build(),
                    OpenFoodFactsProduct.builder().code(tag + "-2").productName("Sem imagem " + tag).build(),
                    OpenFoodFactsProduct.builder().code(tag + "-3").productName("Produto " + tag)
                            .genericNamePt("Bebida láctea fermentada").brands("Marca")
                            .imageFrontUrl("https://img/" + tag + "-3.jpg").build(),
                    OpenFoodFactsProduct.builder().code(tag + "-4").productName("Extra " + tag)
                            .imageFrontUrl("https://img/" + tag + "-4.jpg").build());
        });
    }

    private CatalogImportServiceImpl service(Executor executor) {
        return new CatalogImportServiceImpl(categoryRepository, subCategoryRepository, productRepository,
                productImageRepository, client, eventPublisher, cacheManager, transactionManager, executor);
    }

    @Test
    void import_createsTaxonomyAndProducts_skippingInvalid() {
        cacheManager.getCache("product").put("list", "stale");

        ImportJobDTO started = service(Runnable::run).startOpenFoodFactsImport(2, admin);

        assertEquals("OPEN_FOOD_FACTS", started.getSource());
        assertEquals(12, started.getTotalSteps());
        assertEquals("Administrador", started.getStartedBy());

        assertEquals(4, categoryRepository.count());
        assertEquals(12, subCategoryRepository.count());
        assertEquals(24, productRepository.count());
        assertEquals(24, productImageRepository.count());
        verify(client, times(12)).searchProducts(anyString(), eq(4));
        assertNull(cacheManager.getCache("product").get("list"), "caches invalidados ao terminar");
    }

    @Test
    void import_reportsCountersAndComposesDescriptions() {
        CatalogImportServiceImpl service = service(Runnable::run);

        ImportJobDTO started = service.startOpenFoodFactsImport(2, admin);
        ImportJobDTO done = service.getJob(started.getId());

        assertEquals(ImportJobDTO.Status.COMPLETED, done.getStatus());
        assertEquals(12, done.getCompletedSteps());
        assertEquals(4, done.getCategoriesCreated());
        assertEquals(12, done.getSubCategoriesCreated());
        assertEquals(24, done.getProductsCreated());
        assertEquals(24, done.getImagesCreated());
        assertEquals(24, done.getProductsSkipped());
        assertTrue(done.getErrors().isEmpty());
        assertNotNull(done.getFinishedAt());

        Product oat = productRepository.findBySlug("aveia-em-flocos-nestle-sodas-sodas-1").orElseThrow();
        assertEquals("Aveia Em Flocos Nestlé sodas", oat.getName());
        assertEquals("Marca: Nestlé · 500g · Nutri-Score A", oat.getDescription());

        Product generic = productRepository.findBySlug("produto-sodas-sodas-3").orElseThrow();
        assertEquals("Bebida láctea fermentada", generic.getDescription());

        ProductImage image = productImageRepository.findByProductIdOrderByIdAsc(oat.getId()).get(0);
        assertEquals("https://img/sodas-1.jpg", image.getUrlImage());
        assertEquals("image/jpeg", image.getType());
        assertEquals("external:off:sodas-1", image.getFilename());

        // limite de 2 por subcategoria: o terceiro válido (-4) não entra
        assertFalse(productRepository.findBySlug("extra-sodas-sodas-4").isPresent());

        verify(eventPublisher).publish(eq(EventType.CATALOG_IMPORT_STARTED), eq(started.getId()), anyString(), eq(admin));
        verify(eventPublisher).publish(eq(EventType.CATALOG_IMPORT_COMPLETED), eq(started.getId()), anyString(), eq(admin));
    }

    @Test
    void import_isIdempotent() {
        CatalogImportServiceImpl service = service(Runnable::run);
        service.startOpenFoodFactsImport(2, admin);

        ImportJobDTO second = service.getJob(service.startOpenFoodFactsImport(2, admin).getId());

        assertEquals(ImportJobDTO.Status.COMPLETED, second.getStatus());
        assertEquals(0, second.getCategoriesCreated());
        assertEquals(0, second.getSubCategoriesCreated());
        assertEquals(0, second.getProductsCreated());
        assertEquals(0, second.getImagesCreated());
        assertEquals(48, second.getProductsSkipped());
        assertEquals(4, categoryRepository.count());
        assertEquals(12, subCategoryRepository.count());
        assertEquals(24, productRepository.count());
    }

    @Test
    void import_reusesExistingCategoryBySlug() {
        categoryRepository.save(Category.builder().name("Bebidas").description("já existia").slug("bebidas").build());

        CatalogImportServiceImpl service = service(Runnable::run);
        ImportJobDTO done = service.getJob(service.startOpenFoodFactsImport(1, admin).getId());

        assertEquals(3, done.getCategoriesCreated());
        assertEquals(4, categoryRepository.count());
        assertEquals("já existia", categoryRepository.findBySlug("bebidas").orElseThrow().getDescription());
    }

    @Test
    void import_stepFailureIsRecordedAndJobContinues() {
        when(client.searchProducts(eq("cheeses"), anyInt()))
                .thenThrow(new OpenFoodFactsClient.OpenFoodFactsException("timeout"));

        CatalogImportServiceImpl service = service(Runnable::run);
        ImportJobDTO done = service.getJob(service.startOpenFoodFactsImport(1, admin).getId());

        assertEquals(ImportJobDTO.Status.COMPLETED, done.getStatus());
        assertEquals(List.of("Laticínios › Queijos: timeout"), done.getErrors());
        assertEquals(12, done.getCompletedSteps());
        assertEquals(11, done.getProductsCreated());
    }

    @Test
    void import_onlyOneJobAtATime() {
        List<Runnable> pending = new ArrayList<>();
        CatalogImportServiceImpl service = service(pending::add);

        ImportJobDTO running = service.startOpenFoodFactsImport(1, admin);
        assertEquals(ImportJobDTO.Status.RUNNING, running.getStatus());
        assertEquals(ImportJobDTO.Status.RUNNING, service.getLatestJob().orElseThrow().getStatus());

        assertThrows(BusinessExcepion.class, () -> service.startOpenFoodFactsImport(1, admin));

        pending.get(0).run();
        assertEquals(ImportJobDTO.Status.COMPLETED, service.getJob(running.getId()).getStatus());

        // terminado o primeiro, outro pode começar
        assertNotNull(service.startOpenFoodFactsImport(1, admin));
    }

    @Test
    void jobs_unknownIdIsNotFound_andLatestEmptyBeforeFirstRun() {
        CatalogImportServiceImpl service = service(Runnable::run);

        assertTrue(service.getLatestJob().isEmpty());
        assertThrows(NotFoundException.class, () -> service.getJob("nope"));
    }

    @Test
    void import_skipsProductWithSameNormalizedNameInTheSubCategory() {
        // mesmo produto com outro código (ex.: outra embalagem) e nome só com acento/maiúsculas diferentes
        when(client.searchProducts(anyString(), anyInt())).thenAnswer(inv -> {
            String tag = inv.getArgument(0);
            return List.of(
                    OpenFoodFactsProduct.builder().code(tag + "-1").productName("Café Pilão " + tag)
                            .imageFrontUrl("https://img/" + tag + "-1.jpg").build(),
                    OpenFoodFactsProduct.builder().code(tag + "-2").productName("CAFE  PILAO " + tag)
                            .imageFrontUrl("https://img/" + tag + "-2.jpg").build());
        });
        CatalogImportServiceImpl service = service(Runnable::run);

        ImportJobDTO done = service.getJob(service.startOpenFoodFactsImport(5, admin).getId());

        assertEquals(12, productRepository.count());
        assertEquals(12, done.getProductsCreated());
        assertEquals(12, done.getProductsSkipped());
    }
}
