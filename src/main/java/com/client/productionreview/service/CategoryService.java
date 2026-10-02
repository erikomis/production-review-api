package com.client.productionreview.service;

import com.client.productionreview.model.jpa.Category;

import java.util.List;

public interface CategoryService {

     Category addCategory(Category categorie);

     Category updateCategory(Category categorioDto, Long id);

     void deleteCategory(Long id);

     Category getCategory(Long id);

     Category getCategoryBySlug(String slug);

     List<Category> getAllCategories();
}
