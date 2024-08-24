package com.client.productionreview.service;


import com.client.productionreview.model.jpa.SubCategory;

import java.util.List;

public interface SubCategoryService {

    public SubCategory addSubCategory(SubCategory categorie);

    public SubCategory updateSubCategory(SubCategory category, Long id);

    public void deleteSubCategory(Long id);

    public SubCategory getSubCategory(Long id);

    public List<SubCategory> getAllSubCategorie();
}
