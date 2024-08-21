package com.client.productionreview.service;

import com.client.productionreview.model.Category;

import java.util.List;

public interface CategoryService {

    public Category addCatogory(Category categorie);

    public Category updateCatogory(Category categorioDto, Long id);

    public void deleteCatogory(Long id);

    public Category getCatogory(Long id);

    public List<Category> getAllCatogories();
}
