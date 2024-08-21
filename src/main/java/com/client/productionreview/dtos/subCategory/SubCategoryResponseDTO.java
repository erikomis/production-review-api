package com.client.productionreview.dtos.subCategory;

import lombok.*;

@Getter
@Setter
public class SubCategoryResponseDTO {

    private Long id;

    private String name;

    private String description;

    private String slug;

    private Long categorieId;
}
