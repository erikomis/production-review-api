package com.client.productionreview.dtos.product;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductResponseDTO {

    private Long id;

    private String name;

    private String description;

    private String slug;

    private String imageUrl;

    private Long subCategorieId;

}
