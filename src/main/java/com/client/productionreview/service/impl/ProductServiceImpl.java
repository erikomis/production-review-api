package com.client.productionreview.service.impl;

import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class ProductServiceImpl implements ProductService {


    private final ProductRepository productRepository;

    private final SubCategoryRepository subCategorieRepository;




    public ProductServiceImpl(ProductRepository productRepository, SubCategoryRepository subCategorieRepository) {
        this.productRepository = productRepository;
        this.subCategorieRepository = subCategorieRepository;
    }

    @Override
    @CacheEvict(value = "product", allEntries = true)
    public Product addProduct(Product product) {

        var existsCategorie = subCategorieRepository.findById(product.getSubCategorieId());

        if (existsCategorie.isEmpty()) {
            throw new NotFoundException("SubCategorie not exists");
        }

        return productRepository.save(product);


    }

    @Override
    @CacheEvict(value = "product", allEntries = true, key = "#id")
    public Product updateProduct(Product product, Long id) {
        var existsProduct = productRepository.findById(id);
        if (existsProduct.isEmpty()) {
            throw new NotFoundException("Product not found");
        }

        var existsCategorie = subCategorieRepository.findById(product.getSubCategorieId());

        if (existsCategorie.isEmpty()) {
            throw new NotFoundException("SubCategorie not exists");
        }

        return productRepository.save(product);


    }

    @CacheEvict(value = "product", allEntries = true, key = "#id")
    @Override
    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);

    }

    @Override
    @Cacheable(value = "product", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    @Cacheable(value = "product")
    public Page<Product> getAllProduct(Pageable pageable, String search) {

        if (search == null || search.isEmpty()) {
            return productRepository.findAll(pageable);
        }

        return productRepository.findAllByProduct(search, pageable);
    }

//
//    private String storeFile(MultipartFile file, Long productId) {
//        // Normalize file name
//        String fileName = file.getOriginalFilename();
//        if (fileName == null || fileName.contains("..")) {
//            throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
//        }
//
//        try {
//            // Cria o diretório usando o ID do produto e o nome do arquivo
//            Path targetLocation = this.fileStorageLocation.resolve("product/" + productId + "/" + fileName);
//
//            Files.createDirectories(targetLocation.getParent());
//
//            // Copia o arquivo para o local de destino (substituindo o arquivo existente com o mesmo nome)
//            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
//
//            return "product/" + productId + "/" + fileName;
//        } catch (IOException ex) {
//            throw new IoFileException("Could not store file " + fileName + ". Please try again!");
//        }
//    }
//
//
//    private void deleteFile(String fileName) {
//        if (fileName == null || fileName.isEmpty()) {
//            throw new NotFoundException("No file name provided.");
//        }
//
//        Path targetLocation = fileStorageLocation.resolve(fileName);
//
//        try {
//            if (Files.exists(targetLocation)) {
//                Files.delete(targetLocation);
//            } else {
//                throw new RuntimeException("File not found: " + targetLocation);
//            }
//        } catch (IOException ex) {
//            throw new IoFileException("Could not delete file " + fileName + ". Please try again!");
//        }
//    }


}
