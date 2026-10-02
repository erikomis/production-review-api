package com.client.productionreview.dtos.importer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequestDTO {

    public static final int DEFAULT_PRODUCTS_PER_SUBCATEGORY = 12;

    @Min(value = 1, message = "productsPerSubcategory must be between 1 and 30")
    @Max(value = 30, message = "productsPerSubcategory must be between 1 and 30")
    private Integer productsPerSubcategory;

    public int productsPerSubcategoryOrDefault() {
        return productsPerSubcategory == null ? DEFAULT_PRODUCTS_PER_SUBCATEGORY : productsPerSubcategory;
    }
}
