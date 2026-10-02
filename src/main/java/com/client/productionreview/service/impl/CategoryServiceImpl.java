package com.client.productionreview.service.impl;


import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.CategoryService;

import org.springframework.cache.annotation.CacheEvict;
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

    // a lista de categorias fica no mesmo cache, então tudo é invalidado
    @Override
    @CacheEvict(value = "category", allEntries = true)
    public Category updateCategory(Category category, Long id) {
        Category current = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categorie not found"));

        Optional<Category> exists = categoryRepository.findByName(category.getName());

        if(exists.isPresent() && !exists.get().getId().equals(id)){
            throw new BusinessExcepion("Categorie already exists");
        }

        current.setName(category.getName());
        current.setDescription(category.getDescription());
        current.setSlug(category.getSlug());

       return  categoryRepository.save(current);
    }

    @Override
    @CacheEvict(value = "category", allEntries = true)
    public void deleteCategory(Long id) {

        if(categoryRepository.findById(id).isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        if(subCategoryRepository.existsByCategorieId(id)){
            throw new BusinessExcepion("Já existe subcategorias cadastradas para essa categoria sendo assim não é possivel deletar");
        }

        categoryRepository.deleteById(id);

    }

    @Override
    @Cacheable(value = "category", key = "#id")
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categorie not found"));
    }

    @Override
    @Cacheable(value = "category" )
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


}
