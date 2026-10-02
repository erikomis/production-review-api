package com.client.productionreview.dtos.subCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SubCategoryRequestDTO {


    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Description is required")
    private String description;
    @NotBlank(message = "Slug is required")
    private String slug;

    @NotBlank(message = "CategorieId is required")
    @Pattern(regexp = "[\\s]*[0-9]*[1-9]+",message="Categoria tem que ser um número")
    private Long categorieId;
}
