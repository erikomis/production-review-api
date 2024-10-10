package com.client.productionreview.service.impl;


import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
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
    private final SubCategoryRepository subCategoryRepository;

    CategoryServiceImpl(CategoryRepository categorieRepository,
                        SubCategoryRepository subCategoryRepository) {
        this.categoryRepository = categorieRepository;
        this.subCategoryRepository = subCategoryRepository;
    }


    @Override
    @CacheEvict(value = "category", allEntries = true)
    public Category addCategory(Category category) {
        Optional<Category> exists = categoryRepository.findByName(category.getName());

        if(exists.isPresent()){
            throw new BusinessExcepion("Categorie already exists");
        }

        return categoryRepository.save(category);
    }

    @Override
    @CachePut(value = "category", key = "#id")
    public Category updateCategory(Category category, Long id) {
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
    public void deleteCategory(Long id) {

        var category = categoryRepository.findById(id);

        var subCategories = subCategoryRepository.findByCategorieId(id);

        if(subCategories.isPresent()){
            throw new BusinessExcepion("Já existe subcategorias cadastradas para essa categoria sendo assim não é possivel deletar");
        }

        if(category.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        categoryRepository.deleteById(id);

    }

    @Override
    @Cacheable(value = "category", key = "#id")
    public Category getCategory(Long id) {
        var category = categoryRepository.findById(id);

        if(category.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        return category.get();

    }

    @Override
    @Cacheable(value = "category" )
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {
            category.getSubCategories().size();
        }
        return categories;
    }


}
