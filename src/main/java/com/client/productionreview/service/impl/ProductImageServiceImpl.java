package com.client.productionreview.service.impl;

import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.IoFileException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.ProductImageService;
import com.client.productionreview.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageServiceImpl implements ProductImageService {

    /** Imagens externas (ex.: Open Food Facts) não estão no MinIO; só o registro é removido. */
    public static final String EXTERNAL_PREFIX = "external:";

    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ProductImageRepository productImageRepository;

    private final ProductRepository productRepository;

    private final StorageService storageService;

    private final String url;

    private final String bucketName;

    private final DomainEventPublisher eventPublisher;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository, ProductRepository productRepository,
                                   StorageService storageService,
                                   @Value("${minio.url}") String url,
                                   @Value("${minio.bucket.name}") String bucketName,
                                   DomainEventPublisher eventPublisher) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
        this.storageService = storageService;
        this.url = url;
        this.bucketName = bucketName;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    @CacheEvict(value = "product", allEntries = true)
    public ProductImage createProductImage(MultipartFile file, Long idProduct) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo vazio");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Tipo de arquivo não suportado: " + contentType);
        }

        if (!productRepository.existsById(idProduct)) {
            throw new NotFoundException("Product not found");
        }

        // chave única por produto: evita sobrescrever imagens com o mesmo nome original
        String objectName = "product/" + idProduct + "/" + UUID.randomUUID() + extensionOf(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            var bucket = storageService.uploadFile(bucketName, objectName, inputStream, file.getSize(), contentType);
            ProductImage productImage = new ProductImage();
            productImage.setFilename(objectName);
            productImage.setType(contentType);
            productImage.setUrlImage(url + "/" + bucket.bucket() + "/" + bucket.object());
            productImage.setProductId(idProduct);

            ProductImage saved = productImageRepository.save(productImage);
            eventPublisher.publish(EventType.PRODUCT_IMAGE_ADDED, saved.getId(),
                    "Imagem adicionada ao produto " + idProduct);
            return saved;

        } catch (IOException e) {
            throw new IoFileException("Não foi possível ler o arquivo enviado");
        }
    }

    @Override
    @CacheEvict(value = "product", allEntries = true)
    public void deleteFile(Long idProductImage) {

        ProductImage productImage = productImageRepository.findById(idProductImage)
                .orElseThrow(() -> new NotFoundException("Product Image not found"));

        String filename = productImage.getFilename();
        if (filename == null || !filename.startsWith(EXTERNAL_PREFIX)) {
            storageService.deleteFile(bucketName, filename);
        }

        productImageRepository.deleteById(productImage.getId());
        eventPublisher.publish(EventType.PRODUCT_IMAGE_REMOVED, productImage.getId(),
                "Imagem removida do produto " + productImage.getProductId());
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String extension = filename.substring(dot).toLowerCase();
        // aceita apenas extensões simples, sem caminhos
        return extension.matches("\\.[a-z0-9]{1,5}") ? extension : "";
    }
}
