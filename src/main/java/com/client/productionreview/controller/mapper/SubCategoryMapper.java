package com.client.productionreview.controller.mapper;

import com.client.productionreview.dtos.subCategory.SubCategoryRequestDTO;
import com.client.productionreview.dtos.subCategory.SubCategoryResponseDTO;
import com.client.productionreview.model.jpa.SubCategory;
import org.springframework.stereotype.Component;

@Component
public class SubCategoryMapper {

    public SubCategory toModel(SubCategoryRequestDTO subCategoryDto) {
        SubCategory subCategory = new SubCategory();
        subCategory.setName(subCategoryDto.getName());
        subCategory.setDescription(subCategoryDto.getDescription());
        subCategory.setSlug(subCategoryDto.getSlug());
        subCategory.setCategorieId(subCategoryDto.getCategorieId());
        return subCategory;
    }

    public SubCategoryResponseDTO toDTO(SubCategory subCategory) {
        SubCategoryResponseDTO subCategoryResponseDTO = new SubCategoryResponseDTO();
        subCategoryResponseDTO.setId(subCategory.getId());
        subCategoryResponseDTO.setName(subCategory.getName());
        subCategoryResponseDTO.setSlug(subCategory.getSlug());
        subCategoryResponseDTO.setDescription(subCategory.getDescription());
        subCategoryResponseDTO.setCategorieId(subCategory.getCategorieId());
        return subCategoryResponseDTO;
    }
}
