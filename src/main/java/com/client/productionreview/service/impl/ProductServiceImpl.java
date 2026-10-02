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
    @CacheEvict(value = "product", allEntries = true)
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

        return productRepository.save(current);


    }

    @CacheEvict(value = "product", allEntries = true)
    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
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

}
