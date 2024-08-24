package com.client.productionreview.controller.mapper;

import com.client.productionreview.dtos.product.ProductRequestDTO;
import com.client.productionreview.dtos.product.ProductResponseDTO;
import com.client.productionreview.model.jpa.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toModel(ProductRequestDTO productDto) {
        Product product = new Product();
        product.setName(productDto.getName());
        product.setSlug(productDto.getSlug());
        product.setDescription(productDto.getDescription());
        product.setSubCategorieId(productDto.getSubCategorieId());
        return product;
    }

    public ProductResponseDTO toDTO(Product product) {
        ProductResponseDTO productResponseDTO = new ProductResponseDTO();
        productResponseDTO.setId(product.getId());
        productResponseDTO.setName(product.getName());
        productResponseDTO.setSlug(product.getSlug());
        productResponseDTO.setDescription(product.getDescription());
        productResponseDTO.setSubCategorieId(product.getSubCategorieId());
        return productResponseDTO;
    }
}
