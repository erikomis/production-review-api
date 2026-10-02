package com.client.productionreview.controller.mapper;

import com.client.productionreview.dtos.category.CategoryDetailDTO;
import com.client.productionreview.dtos.category.CategoryRequestDTO;
import com.client.productionreview.dtos.category.CategoryResponseDTO;
import com.client.productionreview.model.jpa.Category;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CategoryMapper {

    public Category toModel(CategoryRequestDTO categorioDto) {
        Category categorie = new Category();
        categorie.setName(categorioDto.getName());
        categorie.setDescription(categorioDto.getDescription());
        categorie.setSlug(categorioDto.getSlug());
        return categorie;
    }

    public CategoryResponseDTO toDTO(Category categorie) {
        CategoryResponseDTO categoryResponseDTO = new CategoryResponseDTO();
        categoryResponseDTO.setId(categorie.getId());
        categoryResponseDTO.setName(categorie.getName());
        categoryResponseDTO.setDescription(categorie.getDescription());
        categoryResponseDTO.setSlug(categorie.getSlug());
        return categoryResponseDTO;
    }

    public CategoryDetailDTO toDetailDTO(Category category) {
        List<CategoryDetailDTO.SubCategoryItem> subCategories = category.getSubCategories() == null ? List.of()
                : category.getSubCategories().stream()
                .sorted(Comparator.comparing(sub -> sub.getName() == null ? "" : sub.getName()))
                .map(sub -> new CategoryDetailDTO.SubCategoryItem(sub.getId(), sub.getName(), sub.getDescription(), sub.getSlug()))
                .toList();
        return new CategoryDetailDTO(category.getId(), category.getName(), category.getDescription(), category.getSlug(),
                subCategories);
    }
}
