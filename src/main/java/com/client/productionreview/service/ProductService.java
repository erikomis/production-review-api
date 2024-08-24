package com.client.productionreview.service;

import com.client.productionreview.model.jpa.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ProductService {


        public Product addProduct(Product product);

        public Product updateProduct(Product product, Long id);

        public void deleteProduct(Long id);

        public  Product getProduct(Long id);

        public Page<Product> getAllProduct(Pageable pageable, String search);
}
