package com.client.productionreview.service.impl;

import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.SubCategoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubCategoryServiceImpl implements SubCategoryService {



    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final DomainEventPublisher eventPublisher;

    SubCategoryServiceImpl(SubCategoryRepository subCategoryRepository, CategoryRepository categoryRepository,
                           DomainEventPublisher eventPublisher){
        this.subCategoryRepository = subCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
    }


    @Override
    @CacheEvict(value = {"subCategory", "category"}, allEntries = true)
    public SubCategory addSubCategory(SubCategory subCategory) {
        Optional<Category> existsCategorie = getExistsCategorie(subCategory.getCategorieId());

        if (existsCategorie.isEmpty()) {
            throw new NotFoundException("Categorie not exists");
        }

        Optional<SubCategory> existsName = getExistsSubCategoria(subCategory);

        if (existsName.isPresent()) {
            throw new BusinessExcepion("SubCategorie  exists already");
        }


        SubCategory saved = subCategoryRepository.save(subCategory);
        eventPublisher.publish(EventType.SUBCATEGORY_CREATED, saved.getId(), "Subcategoria " + saved.getName() + " criada");
        return saved;
    }


    // a listagem de produtos traz o nome da subcategoria
    @Override
    @CacheEvict(value = {"subCategory", "category", "product"}, allEntries = true)
    public SubCategory updateSubCategory(SubCategory subCategorie, Long id) {
        Optional<Category> existsCategorie= getExistsCategorie(subCategorie.getCategorieId());
        if (existsCategorie.isEmpty()) {
            throw new NotFoundException("Categorie not exists");
        }

       Optional<SubCategory>existsId = subCategoryRepository.findById(id);

        if (existsId.isEmpty()) {
            throw new NotFoundException("SubCategorie not found");
        }

        Optional<SubCategory> existsName = getExistsSubCategoria(subCategorie);

        if (existsName.isPresent() && !existsName.get().getId().equals(id)) {
            throw new BusinessExcepion("SubCategorie  exists already");
        }

        SubCategory subCategory = existsId.get();
        subCategory.setName(subCategorie.getName());
        subCategory.setDescription(subCategorie.getDescription());
        subCategory.setSlug(subCategorie.getSlug());
        subCategory.setCategorieId(subCategorie.getCategorieId());

        SubCategory saved = subCategoryRepository.save(subCategory);
        eventPublisher.publish(EventType.SUBCATEGORY_UPDATED, saved.getId(), "Subcategoria " + saved.getName() + " atualizada");
        return saved;
    }

    @Override
    @CacheEvict(value = {"subCategory", "category", "product"}, allEntries = true)
    public void deleteSubCategory(Long id) {
        var existsId = subCategoryRepository.findById(id);
        if (existsId.isEmpty()) {
            throw new NotFoundException("SubCategorie not found");
        }

        subCategoryRepository.deleteById(id);
        eventPublisher.publish(EventType.SUBCATEGORY_DELETED, id, "Subcategoria " + existsId.get().getName() + " excluída");
    }

    @Override
    @Cacheable(value = "subCategory", key = "#id")
    public SubCategory getSubCategory(Long id) {
        var existsId = subCategoryRepository.findById(id);
        if (existsId.isEmpty()) {
            throw new NotFoundException("SubCategorie not found");
        }
        return existsId.get();
    }

    @Override
    @Cacheable(value = "subCategory")
    public List<SubCategory> getAllSubCategorie() {

        List<SubCategory> subCategories = subCategoryRepository.findAll();

        return  subCategories;
    }


    private Optional<SubCategory> getExistsSubCategoria(SubCategory subCategory) {
        return subCategoryRepository.findByName(subCategory.getName());

    }

    private Optional<Category> getExistsCategorie(Long id) {
        return categoryRepository.findById(id);

    }
}
