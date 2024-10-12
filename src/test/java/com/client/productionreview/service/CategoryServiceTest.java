package com.client.productionreview.service;

import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryServiceImpl categoryService;


    @Mock
    private CategoryRepository categorieRepository;

    @Mock
    private  SubCategoryRepository subCategoryRepository;


    @Test
    void  givenCategory_whenAddCategory_thenReturnCategory() {

        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.save(category)).thenReturn(category);

        assertEquals(category, categoryService.addCategory(category));

        verify(categorieRepository, times(1)).save(category);

    }

    @Test
    void  givenCategory_whenAddCategory_thenThrowException() {

        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");


        when(categorieRepository.findByName(category.getName())).thenReturn(java.util.Optional.of(category));

        assertThrows(BusinessExcepion.class, () -> {
            categoryService.addCategory(category);
        });

        verify(categorieRepository, times(1)).findByName(category.getName());
        verify(categorieRepository, times(0)).save(category);

    }

    @Test
    void  givenCategory_whenUpdateCategory_thenReturnCategory() {

        Long id = 1L;
        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.findById(id)).thenReturn(java.util.Optional.of(category));
        when(categorieRepository.save(category)).thenReturn(category);

        categoryService.updateCategory(category, id);

        verify(categorieRepository, times(1)).findById(id);
        verify(categorieRepository, times(1)).findByName(category.getName());
        verify(categorieRepository, times(1)).save(category);

    }

    @Test
    void  givenCategory_whenUpdateCategory_thenThrowExceptionNotFound() {

        Long id = 1L;
        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            categoryService.updateCategory(category, id);
        });

        verify(categorieRepository, times(1)).findById(id);
        verify(categorieRepository, times(0)).findByName(category.getName());
        verify(categorieRepository, times(0)).save(category);

    }

    @Test
    void  givenCategory_whenUpdateCategory_thenThrowExceptionBusinessExcepion() {

        Long id = 1L;
        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.findById(id)).thenReturn(java.util.Optional.of(category));
        when(categorieRepository.findByName(category.getName())).thenReturn(java.util.Optional.of(category));

        assertThrows(BusinessExcepion.class, () -> {
            categoryService.updateCategory(category, id);
        });

        verify(categorieRepository, times(1)).findById(id);
        verify(categorieRepository, times(1)).findByName(category.getName());
        verify(categorieRepository, times(0)).save(category);

    }




    @Test
    void  givenCategoryId_whenDeleteCategory_thenDeleteCategory() {

        Long id = 1L;
        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.findById(id)).thenReturn(java.util.Optional.of(category));

        categoryService.deleteCategory(id);

        verify(categorieRepository, times(1)).findById(id);
        verify(categorieRepository, times(1)).deleteById(id);

    }

    @Test
    void  givenCategoryId_whenDeleteCategory_thenThrowExceptionNotFound() {

        Long id = 1L;
        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.findById(id)).thenReturn(java.util.Optional.empty());


        assertThrows(NotFoundException.class, () -> {
            categoryService.deleteCategory(id);
        });

        verify(categorieRepository, times(1)).findById(id);
        verify(categorieRepository, times(0)).deleteById(id);

    }

    @Test
    void  givenCategoryId_whenDeleteCategory_thenThrowExceptionBusiness() {

        Long id = 1L;
        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        SubCategory subCategory = new SubCategory();
        subCategory.setId(1L);
        subCategory.setName("subCategory");

        when(categorieRepository.findById(id)).thenReturn(java.util.Optional.of(category));
        when(subCategoryRepository.findByCategorieId(id)).thenReturn(java.util.Optional.of(subCategory));

        assertThrows(BusinessExcepion.class, () -> {
            categoryService.deleteCategory(id);
        });

        verify(categorieRepository, times(1)).findById(id);
        verify(categorieRepository, times(0)).deleteById(id);

    }


    @Test
    void  givenCategoryId_whenGetCategory_thenReturnCategory() {

        Long id = 1L;
        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.findById(id)).thenReturn(java.util.Optional.of(category));

        categoryService.getCategory(id);

        verify(categorieRepository, times(1)).findById(id);

    }
    @Test
    void  givenCategoryId_whenGetCategory_thenThrowException() {

        Long id = 1L;
        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            categoryService.getCategory(id);
        });

        verify(categorieRepository, times(1)).findById(id);

    }

    @Test
    void  givenCategoryId_whenGetAllCategory_thenReturnCategory() {

        Category category = new Category();
        category.setName("category");
        category.setDescription("description");
        category.setSlug("slug");

        when(categorieRepository.findAll()).thenReturn(List.of(category));

        assertEquals(List.of(category), categoryService.getAllCategories());

        verify(categorieRepository, times(1)).findAll();

    }

}
