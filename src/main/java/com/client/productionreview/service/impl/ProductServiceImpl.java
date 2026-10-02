package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.product.ProductDetailDTO;
import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductImageSummaryDTO;
import com.client.productionreview.dtos.product.ProductSortProperty;
import com.client.productionreview.dtos.product.ProductSuggestionDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.utils.TextNormalizer;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ProductServiceImpl implements ProductService {

    static final int SUGGEST_MIN_LENGTH = 2;

    static final int SUGGEST_MAX_LIMIT = 10;


    private final ProductRepository productRepository;

    private final SubCategoryRepository subCategorieRepository;

    private final ProductImageRepository productImageRepository;

    private final DomainEventPublisher eventPublisher;


    public ProductServiceImpl(ProductRepository productRepository, SubCategoryRepository subCategorieRepository,
                              ProductImageRepository productImageRepository, DomainEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.subCategorieRepository = subCategorieRepository;
        this.productImageRepository = productImageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @CacheEvict(value = {"product", "seo"}, allEntries = true)
    public Product addProduct(Product product) {

        var existsCategorie = subCategorieRepository.findById(product.getSubCategorieId());

        if (existsCategorie.isEmpty()) {
            throw new NotFoundException("SubCategorie not exists");
        }

        Product saved = productRepository.save(product);
        eventPublisher.publish(EventType.PRODUCT_CREATED, saved.getId(), "Produto " + saved.getName() + " criado");
        return saved;
    }

    @Override
    @CacheEvict(value = {"product", "seo"}, allEntries = true)
    public Product updateProduct(Product product, Long id) {
        Product current = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        var existsCategorie = subCategorieRepository.findById(product.getSubCategorieId());

        if (existsCategorie.isEmpty()) {
            throw new NotFoundException("SubCategorie not exists");
        }

        current.setName(product.getName());
        current.setDescription(product.getDescription());
        current.setSlug(product.getSlug());
        current.setSubCategorieId(product.getSubCategorieId());

        Product saved = productRepository.save(current);
        eventPublisher.publish(EventType.PRODUCT_UPDATED, saved.getId(), "Produto " + saved.getName() + " atualizado");
        return saved;
    }

    @CacheEvict(value = {"product", "seo"}, allEntries = true)
    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        productRepository.delete(product);
        eventPublisher.publish(EventType.PRODUCT_DELETED, id, "Produto " + product.getName() + " excluído");
    }

    @Override
    @Cacheable(value = "product", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    @Cacheable(value = "product", key = "'slug:' + #slug")
    public Product getProductBySlug(String slug) {
        return productRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    @Cacheable(value = "product", key = "'detail:' + #id")
    public ProductDetailDTO getProductDetail(Long id) {
        return detail(ProductFilter.byId(id));
    }

    @Override
    @Cacheable(value = "product", key = "'detail-slug:' + #slug")
    public ProductDetailDTO getProductDetailBySlug(String slug) {
        return detail(ProductFilter.bySlug(slug));
    }

    private ProductDetailDTO detail(ProductFilter filter) {
        ProductSummaryDTO summary = productRepository.findSummaries(filter, PageRequest.of(0, 1))
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Product not found"));

        List<ProductImageSummaryDTO> images = productImageRepository.findByProductIdOrderByIdAsc(summary.getId()).stream()
                .map(image -> new ProductImageSummaryDTO(image.getId(), image.getUrlImage()))
                .toList();

        return ProductDetailDTO.from(summary, images);
    }

    @Override
    public List<ProductSuggestionDTO> suggest(String q, int limit) {
        String term = TextNormalizer.normalize(q);
        if (term == null || term.length() < SUGGEST_MIN_LENGTH) {
            throw new BadRequestException("q: informe pelo menos " + SUGGEST_MIN_LENGTH + " caracteres");
        }
        if (limit < 1 || limit > SUGGEST_MAX_LIMIT) {
            throw new BadRequestException("limit: deve estar entre 1 e " + SUGGEST_MAX_LIMIT);
        }
        return productRepository.suggest(term, limit);
    }

    @Override
    @Cacheable(value = "product", key = "'list:' + #filter.toString() + ':' + #pageable.toString()")
    public Page<ProductSummaryDTO> listProducts(ProductFilter filter, Pageable pageable) {
        // valida antes de montar a consulta: propriedade desconhecida vira 400
        pageable.getSort().forEach(order -> ProductSortProperty.from(order.getProperty()));
        return productRepository.findSummaries(filter, pageable);
    }

}
