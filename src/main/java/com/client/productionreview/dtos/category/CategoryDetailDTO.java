package com.client.productionreview.dtos.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDetailDTO {

    private Long id;
    private String name;
    private String description;
    private String slug;
    private List<SubCategoryItem> subCategories = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubCategoryItem {
        private Long id;
        private String name;
        private String description;
        private String slug;
    }
}
