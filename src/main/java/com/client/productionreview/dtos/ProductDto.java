package com.client.productionreview.dtos;

import com.client.productionreview.model.SubCategorie;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductDto {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")

    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Slug is required")
    private String slug;


    @NotBlank(message = "Categoria é obrigatória")
    private Long subCategorieId;
}
