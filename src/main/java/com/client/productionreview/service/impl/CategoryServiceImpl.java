package com.client.productionreview.service.impl;


import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.CategoryService;
import com.client.productionreview.service.DomainEventPublisher;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class CategoryServiceImpl implements CategoryService {


    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final DomainEventPublisher eventPublisher;

    CategoryServiceImpl(CategoryRepository categorieRepository,
                        SubCategoryRepository subCategoryRepository,
                        DomainEventPublisher eventPublisher) {
        this.categoryRepository = categorieRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.eventPublisher = eventPublisher;
    }


    @Override
    @CacheEvict(value = "category", allEntries = true)
    public Category addCategory(Category category) {
        Optional<Category> exists = categoryRepository.findByName(category.getName());

        if(exists.isPresent()){
            throw new BusinessExcepion("Categorie already exists");
        }

        Category saved = categoryRepository.save(category);
        eventPublisher.publish(EventType.CATEGORY_CREATED, saved.getId(), "Categoria " + saved.getName() + " criada");
        return saved;
    }

    // a lista de categorias fica no mesmo cache, então tudo é invalidado;
    // a listagem de produtos traz o nome da categoria
    @Override
    @CacheEvict(value = {"category", "product"}, allEntries = true)
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

        Category saved = categoryRepository.save(current);
        eventPublisher.publish(EventType.CATEGORY_UPDATED, saved.getId(), "Categoria " + saved.getName() + " atualizada");
        return saved;
    }

    @Override
    @CacheEvict(value = {"category", "product"}, allEntries = true)
    public void deleteCategory(Long id) {

        Category current = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categorie not found"));

        if(subCategoryRepository.existsByCategorieId(id)){
            throw new BusinessExcepion("Já existe subcategorias cadastradas para essa categoria sendo assim não é possivel deletar");
        }

        categoryRepository.deleteById(id);
        eventPublisher.publish(EventType.CATEGORY_DELETED, id, "Categoria " + current.getName() + " excluída");
    }

    @Override
    @Cacheable(value = "category", key = "#id")
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categorie not found"));
    }

    @Override
    @Cacheable(value = "category", key = "'slug:' + #slug")
    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Categorie not found"));
    }

    @Override
    @Cacheable(value = "category" )
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


}
