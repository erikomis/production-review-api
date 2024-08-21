package com.client.productionreview.service.impl;


import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.Category;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.service.CategoryService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class CategoryServiceImpl implements CategoryService {


    private final CategoryRepository categoryRepository;

    CategoryServiceImpl(CategoryRepository categorieRepository){
        this.categoryRepository = categorieRepository;
    }


    @Override
    public Category addCatogory(Category category) {
        Optional<Category> exists = categoryRepository.findByName(category.getName());

        if(exists.isPresent()){
            throw new BusinessExcepion("Categorie already exists");
        }

        return categoryRepository.save(category);
    }

    @Override
    public Category updateCatogory(Category category, Long id) {
       Optional<Category> categoryExist = categoryRepository.findById(id);
        if(categoryExist.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        Optional<Category> exists = categoryRepository.findByName(category.getName());

        if(exists.isPresent()){
            throw new BusinessExcepion("Categorie already exists");
        }

        category.setId(id);

       return  categoryRepository.save(category);
    }

    @Override
    public void deleteCatogory(Long id) {

        var category = categoryRepository.findById(id);

        if(category.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        categoryRepository.deleteById(id);

    }

    @Override
    public Category getCatogory(Long id) {
        var category = categoryRepository.findById(id);

        if(category.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        return category.get();

    }

    @Override
    public List<Category> getAllCatogories() {
        return categoryRepository.findAll();
    }


}
