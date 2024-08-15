package com.client.productionreview.service.impl;

import com.client.productionreview.config.FileStorageProperties;
import com.client.productionreview.dtos.ProductDto;
import com.client.productionreview.exception.IoFileException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.Product;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.SubCategorieRepository;
import com.client.productionreview.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;



@Service
public class ProductServiceImpl implements ProductService {


    private final ProductRepository productRepository;

    private final SubCategorieRepository subCategorieRepository;

    private final Path fileStorageLocation;


    ProductServiceImpl(ProductRepository productRepository, SubCategorieRepository subCategorieRepository, FileStorageProperties fileStorageProperties) {
        this.productRepository = productRepository;
        this.subCategorieRepository = subCategorieRepository;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();

    }


    @Override
    public Product addProduct(ProductDto product, MultipartFile file) {

        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/png") && !contentType.equals("image/jpeg"))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Apenas arquivos PNG ou JPG são permitidos");
        }

        var existsCategorie = subCategorieRepository.findById(product.getSubCategorieId());

        if(existsCategorie.isEmpty()){
            throw new NotFoundException("SubCategorie not exists");
        }

        Product productEntity = productRepository.save(Product.builder().
                subCategorie(existsCategorie.get())
                .name(product.getName())
                .description(product.getDescription())
                .slug(product.getSlug())
                .build());


        productRepository.save(productEntity);


        String fileUrl = storeFile(file, product.getId());

        productEntity.setImageUrl(fileUrl);

        productRepository.save(productEntity);
        return productEntity;



    }

    @Override
    public Product updateProduct(ProductDto product, Long id,  MultipartFile file) {

        var existsProduct = productRepository.findById(id);
        if (existsProduct.isEmpty()){
            throw new NotFoundException("Product not found");
        }

        var existsCategorie = subCategorieRepository.findById(product.getSubCategorieId());

        if(existsCategorie.isEmpty()){
            throw new NotFoundException("SubCategorie not exists");
        }

        if (file.isEmpty()){

            return  productRepository.save(
                    Product.builder()
                            .id(id)
                            .name(product.getName())
                            .description(product.getDescription())
                            .slug(product.getSlug())
                            .subCategorie(existsProduct.get().getSubCategorie())
                            .build()
            );
        } else {
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.equals("image/png") && !contentType.equals("image/jpeg"))) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Apenas arquivos PNG ou JPG são permitidos");
            }

            deleteFile(existsProduct.get().getImageUrl());

            String fileUrl = storeFile(file, id);

            return productRepository.save(
                    Product.builder()
                            .id(id)
                            .name(product.getName())
                            .description(product.getDescription())
                            .slug(product.getSlug())
                            .subCategorie(existsCategorie.get())
                            .imageUrl(fileUrl)
                            .build()
            );
        }





    }

    @Override
    public void deleteProduct(Long id) {

        var product = productRepository.findById(id);

        if(product.isEmpty()){
            throw new NotFoundException("Product not found");
        }

        deleteFile(product.get().getImageUrl());

        productRepository.delete(product.get());

    }

    @Override
    public Product getProduct(Long id) {
       var existsProduct = productRepository.findById(id);

       if (existsProduct.isEmpty()){
           throw new NotFoundException("Product not found");
       }

         return existsProduct.get();
    }

    @Override
    public Page<Product> getAllProduct(Pageable pageable, String search) {
        return productRepository.findAllByProduct( search,pageable );
    }




    private String storeFile(MultipartFile file, Long productId) {
        // Normalize file name
        String fileName = file.getOriginalFilename();

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Create the directory path using product ID and file name
            Path targetLocation = this.fileStorageLocation.resolve("product/" + productId + "/" + fileName);
            Files.createDirectories(targetLocation.getParent());

            // Copy file to the target location (Replacing existing file with the same name)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "product/" + productId + "/" + fileName;
        } catch (IOException ex) {
            throw new IoFileException("Could not store file " + fileName + ". Please try again!");
        }
    }


    private  void deleteFile(String fileName) {
        Path targetLocation = fileStorageLocation.resolve(fileName);
        System.out.println(targetLocation);
        try {
            Files.delete(targetLocation);
            System.out.println("File deleted successfully");
        } catch (IOException ex) {
            ex.printStackTrace();
           throw new IoFileException("Could not delete file " + fileName + ". Please try again!");
        }
    }



}
