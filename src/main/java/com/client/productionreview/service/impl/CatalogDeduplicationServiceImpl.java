package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.admin.DeduplicationResultDTO;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ProductFollowRepository;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.CatalogDeduplicationService;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CatalogDeduplicationServiceImpl implements CatalogDeduplicationService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductFollowRepository productFollowRepository;
    private final ReviewRepository reviewRepository;
    private final StorageService storageService;
    private final DomainEventPublisher eventPublisher;
    private final String bucketName;

    public CatalogDeduplicationServiceImpl(ProductRepository productRepository, ProductImageRepository productImageRepository,
                                           ProductFollowRepository productFollowRepository, ReviewRepository reviewRepository,
                                           StorageService storageService, DomainEventPublisher eventPublisher,
                                           @Value("${minio.bucket.name:production-review}") String bucketName) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productFollowRepository = productFollowRepository;
        this.reviewRepository = reviewRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
        this.bucketName = bucketName;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"product", "seo"}, allEntries = true)
    public DeduplicationResultDTO deduplicate(User admin) {
        // a consulta já vem ordenada: subcategoria, nome normalizado, mais antigo primeiro
        Map<String, List<Product>> groups = new LinkedHashMap<>();
        for (Product product : productRepository.findDuplicates()) {
            groups.computeIfAbsent(product.getSubCategorieId() + "|" + product.getSearchName(), key -> new ArrayList<>())
                    .add(product);
        }

        DeduplicationResultDTO result = new DeduplicationResultDTO();
        for (List<Product> group : groups.values()) {
            if (group.size() < 2) {
                continue;
            }
            result.setGroups(result.getGroups() + 1);
            result.getKeptIds().add(group.get(0).getId());
            for (Product duplicate : group.subList(1, group.size())) {
                if (reviewRepository.existsByProductId(duplicate.getId())) {
                    // produto com avaliações nunca é apagado automaticamente
                    continue;
                }
                remove(duplicate);
                result.getRemovedIds().add(duplicate.getId());
            }
        }
        result.setRemoved(result.getRemovedIds().size());

        String adminName = admin != null && admin.getName() != null ? admin.getName().trim() : "Administrador";
        eventPublisher.publish(EventType.CATALOG_DEDUPLICATED, null, adminName + " removeu " + result.getRemoved()
                + " produtos duplicados (" + result.getGroups() + " grupos)", admin);
        return result;
    }

    private void remove(Product product) {
        for (ProductImage image : productImageRepository.findByProductIdOrderByIdAsc(product.getId())) {
            String filename = image.getFilename();
            if (filename != null && !filename.startsWith(ProductImageServiceImpl.EXTERNAL_PREFIX)) {
                try {
                    storageService.deleteFile(bucketName, filename);
                } catch (Exception e) {
                    log.warn("Imagem {} do produto {} não removida do storage: {}", filename, product.getId(), e.getMessage());
                }
            }
            productImageRepository.delete(image);
        }
        productFollowRepository.deleteByProducts(List.of(product.getId()));
        productRepository.delete(product);
    }
}
