package com.client.productionreview.service;

import com.client.productionreview.dtos.product.ProductDetailDTO;
import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.model.jpa.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ProductService {


        public Product addProduct(Product product);

        public Product updateProduct(Product product, Long id);

        public void deleteProduct(Long id);

        public  Product getProduct(Long id);

        public Product getProductBySlug(String slug);

        public ProductDetailDTO getProductDetail(Long id);

        public ProductDetailDTO getProductDetailBySlug(String slug);

        /** Autocompletar: {@code q} com pelo menos 2 caracteres (400 se menor); {@code limit} entre 1 e 10. */
        public java.util.List<com.client.productionreview.dtos.product.ProductSuggestionDTO> suggest(String q, int limit);

        /** Página de produtos com nota média e total de reviews visíveis. */
        public Page<ProductSummaryDTO> listProducts(ProductFilter filter, Pageable pageable);
}
