package com.client.productionreview.service.impl;


import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.service.CategoryService;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
    @CacheEvict(value = "category", allEntries = true)
    public Category addCatogory(Category category) {
        Optional<Category> exists = categoryRepository.findByName(category.getName());

        if(exists.isPresent()){
            throw new BusinessExcepion("Categorie already exists");
        }

        return categoryRepository.save(category);
    }

    @Override
    @CachePut(value = "category", key = "#id")
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
    @CacheEvict(value = "category", allEntries = true, key = "#id")
    public void deleteCatogory(Long id) {

        var category = categoryRepository.findById(id);

        if(category.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        categoryRepository.deleteById(id);

    }

    @Override
    @Cacheable(value = "category", key = "#id")
    public Category getCatogory(Long id) {
        var category = categoryRepository.findById(id);

        if(category.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        return category.get();

    }

    @Override
    @Cacheable(value = "category" )
    public List<Category> getAllCatogories() {
        List<Category> categories = categoryRepository.findAll();

        // Garantir que todas as subcategorias sejam carregadas para evitar LazyInitializationException
        for (Category category : categories) {
            category.getSubCategories().size();  // Força o carregamento das subcategorias
        }
        return categories;
    }


}
