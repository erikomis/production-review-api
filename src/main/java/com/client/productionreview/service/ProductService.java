package com.client.productionreview.service;

import com.client.productionreview.dtos.ProductDto;
import com.client.productionreview.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {


        public Product addProduct(ProductDto product, MultipartFile file);

        public Product updateProduct(ProductDto product, Long id,  MultipartFile file);

        public void deleteProduct(Long id);

        public  Product getProduct(Long id);

        public Page<Product> getAllProduct(Pageable pageable, String search);
}
