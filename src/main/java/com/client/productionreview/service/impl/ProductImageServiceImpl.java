package com.client.productionreview.service.impl;

import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.service.ProductImageService;
import com.client.productionreview.service.StorageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class ProductImageServiceImpl implements ProductImageService {


    ProductImageRepository productImageRepository;

    StorageService storageService;

    @Value("${minio.url}")
    private String url;

    @Value("${minio.bucket.name}")
    private String bucketName;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository, StorageService storageService) {
        this.productImageRepository = productImageRepository;
        this.storageService = storageService;
    }

    @Transactional
    @Override
    public ProductImage createProductImage(MultipartFile file, Long idProduct) {

        try (InputStream inputStream = file.getInputStream()) {
            var bucket = storageService.uploadFile(bucketName, file.getOriginalFilename(), inputStream, file.getContentType());
            ProductImage productImage = new ProductImage();
            productImage.setFilename(file.getOriginalFilename());
            productImage.setUrlImage(url+"/"+bucket.bucket()+"/"+bucket.object());
            productImage.setProductId(idProduct);

            return productImageRepository.save(productImage);

        } catch (IOException e) {
            throw new NotFoundException(e.getMessage());

        }
    }

    @Override
    public void deleteFile(Long idProductImage) {

        Optional<ProductImage> productImage = productImageRepository.findById(idProductImage);

        if(productImage.isEmpty()){
            throw new NotFoundException("Product Image not found");
        }

        storageService.deleteFile(bucketName,productImage.get().getFilename());

        productImageRepository.deleteById(productImage.get().getId());
    }
}
