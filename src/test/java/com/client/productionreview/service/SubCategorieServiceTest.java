package com.client.productionreview.service;

import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.Category;
import com.client.productionreview.model.SubCategory;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.service.impl.SubCategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubCategorieServiceTest {

    @InjectMocks
    private SubCategoryServiceImpl subCategorieService;


    @Mock
    private SubCategoryRepository subCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;


    @Test
    void givenSubCategory_whenAddSubCategory_thenReturnSubCategory() {


        Category category = new Category();
        category.setId(1L);
        category.setName("category");
        category.setDescription("description");


        SubCategory subCategory = new SubCategory();
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);


        when(categoryRepository.findById(category.getId())).thenReturn(java.util.Optional.of(category));
        when(subCategoryRepository.findByName(subCategory.getName())).thenReturn(Optional.empty());
        when(subCategoryRepository.save(subCategory)).thenReturn(subCategory);
        SubCategory subCategory1 = subCategorieService.addSubCategory(subCategory);
        assertEquals(subCategory1.getName(), subCategory.getName());
        assertEquals(subCategory1.getDescription(), subCategory.getDescription());
        assertEquals(subCategory1.getSlug(), subCategory.getSlug());
        assertEquals(subCategory1.getCategorieId(), subCategory.getCategorieId());

        verify(subCategoryRepository, times(1)).save(subCategory);
        verify(subCategoryRepository, times(1)).findByName(subCategory.getName());
        verify(categoryRepository, times(1)).findById(category.getId());


    }

    @Test
    void givenSubCategory_whenAddSubCategory_thenThrowExceptionNotFoundException() {

        Category category = new Category();
        category.setId(2L);
        category.setName("category");
        category.setDescription("description");

        SubCategory subCategory = new SubCategory();
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> subCategorieService.addSubCategory(subCategory));

        verify(categoryRepository, times(1)).findById(1L);
        verify(subCategoryRepository, times(0)).findByName(subCategory.getName());
        verify(subCategoryRepository, times(0)).save(subCategory);

    }

    @Test
    void givenSubCategory_whenAddSubCategory_thenThrowExceptionBusinessExcepion() {

        Category category = new Category();
        category.setId(2L);
        category.setName("category");
        category.setDescription("description");

        SubCategory subCategory = new SubCategory();
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(subCategoryRepository.findByName(subCategory.getName())).thenReturn(Optional.of(subCategory));

        assertThrows(BusinessExcepion.class, () -> subCategorieService.addSubCategory(subCategory));

        verify(categoryRepository, times(1)).findById(1L);
        verify(subCategoryRepository, times(1)).findByName(subCategory.getName());
        verify(subCategoryRepository, times(0)).save(subCategory);

    }

    @Test
    void givenSubCategory_whenUpdateSubCategory_thenReturnSubCategory() {

        Category category = new Category();
        category.setId(1L);
        category.setName("category");
        category.setDescription("description");

        SubCategory subCategory = new SubCategory();
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);


        when(categoryRepository.findById(subCategory.getCategorieId())).thenReturn(Optional.of(category));
        when(subCategoryRepository.findByName(subCategory.getName())).thenReturn(Optional.empty());
        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));
        when(subCategoryRepository.save(subCategory)).thenReturn(subCategory);

        SubCategory subCategory1 = subCategorieService.updateSubCategory(subCategory, 1L);
        assertEquals(subCategory1.getName(), subCategory.getName());
        assertEquals(subCategory1.getDescription(), subCategory.getDescription());
        assertEquals(subCategory1.getSlug(), subCategory.getSlug());
        assertEquals(subCategory1.getCategorieId(), subCategory.getCategorieId());

        verify(categoryRepository, times(1)).findById(subCategory.getCategorieId());
        verify(subCategoryRepository, times(1)).findByName(subCategory.getName());
        verify(subCategoryRepository, times(1)).findById(1L);
        verify(subCategoryRepository, times(1)).save(subCategory);
    }

    @Test
    void givenSubCategory_whenUpdateSubCategory_thenThrowExceptionNotFoundExceptionCategory() {

        Category category = new Category();
        category.setId(1L);
        category.setName("category");
        category.setDescription("description");

        SubCategory subCategory = new SubCategory();
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);

        when(categoryRepository.findById(subCategory.getCategorieId())).thenReturn(Optional.empty());


        assertThrows(NotFoundException.class, () -> {
            subCategorieService.updateSubCategory(subCategory, 1L);
        });

        verify(categoryRepository, times(1)).findById(subCategory.getCategorieId());
        verify(subCategoryRepository, times(0)).findByName(subCategory.getName());
        verify(subCategoryRepository, times(0)).findById(1L);
        verify(subCategoryRepository, times(0)).save(subCategory);
    }

    @Test
    void givenSubCategory_whenUpdateSubCategory_thenThrowExceptionNotFoundExceptionSubCategory() {

        Category category = new Category();
        category.setId(1L);
        category.setName("category");
        category.setDescription("description");

        SubCategory subCategory = new SubCategory();
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);

        when(categoryRepository.findById(subCategory.getCategorieId())).thenReturn(Optional.of(category));
        when(subCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            subCategorieService.updateSubCategory(subCategory, 1L);
        });

        verify(categoryRepository, times(1)).findById(subCategory.getCategorieId());
        verify(subCategoryRepository, times(1)).findById(1L);
        verify(subCategoryRepository, times(0)).findByName(subCategory.getName());
        verify(subCategoryRepository, times(0)).save(subCategory);
    }


    @Test
    void givenSubCategory_whenUpdateSubCategory_thenThrowExceptionBusinessExcepion() {

        Category category = new Category();
        category.setId(1L);
        category.setName("category");
        category.setDescription("description");

        SubCategory subCategory = new SubCategory();
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);

        when(categoryRepository.findById(subCategory.getCategorieId())).thenReturn(Optional.of(category));
        when(subCategoryRepository.findByName(subCategory.getName())).thenReturn(Optional.of(subCategory));
        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));



        assertThrows(BusinessExcepion.class, () -> {
            subCategorieService.updateSubCategory(subCategory, 1L);
        });
        verify(categoryRepository, times(1)).findById(subCategory.getCategorieId());
        verify(subCategoryRepository, times(1)).findByName(subCategory.getName());
        verify(categoryRepository, times(1)).findById(subCategory.getCategorieId());
        verify(subCategoryRepository, times(0)).save(subCategory);


    }

    @Test
    void givenSubCategory_whenDeleteSubCategory_thenDeleteSubCategory() {

        SubCategory subCategory = new SubCategory();
        subCategory.setId(1L);
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);

        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));

        subCategorieService.deleteSubCategory(1L);

        verify(subCategoryRepository, times(1)).findById(1L);
        verify(subCategoryRepository, times(1)).deleteById(1L);


    }


    @Test
    void givenSubCategory_whenDeleteSubCategory_thenThrowExceptionNotFoundException() {

        when(subCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            subCategorieService.deleteSubCategory(1L);
        });

        verify(subCategoryRepository, times(1)).findById(1L);
        verify(subCategoryRepository, times(0)).deleteById(1L);
    }


    @Test
    void givenSubCategory_whenGetSubCategory_thenReturnSubCategory() {

        SubCategory subCategory = new SubCategory();
        subCategory.setId(1L);
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);

        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));

        SubCategory subCategory1 = subCategorieService.getSubCategory(1L);

        assertEquals(subCategory1.getId(), subCategory.getId());
        assertEquals(subCategory1.getName(), subCategory.getName());
        assertEquals(subCategory1.getDescription(), subCategory.getDescription());
        assertEquals(subCategory1.getSlug(), subCategory.getSlug());
        assertEquals(subCategory1.getCategorieId(), subCategory.getCategorieId());

        verify(subCategoryRepository, times(1)).findById(1L);

    }

    @Test
    void givenSubCategory_whenGetSubCategory_thenThrowExceptionNotFoundException() {

        when(subCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            subCategorieService.getSubCategory(1L);
        });

        verify(subCategoryRepository, times(1)).findById(1L);
    }


    @Test
    void givenSubCategory_whenGetSubCategory_thenReturnSubCategoryAll() {

        SubCategory subCategory = new SubCategory();
        subCategory.setId(1L);
        subCategory.setName("subCategory1");
        subCategory.setDescription("description1");
        subCategory.setSlug("slug1");
        subCategory.setCategorieId(1L);

        when(subCategoryRepository.findAll()).thenReturn(List.of(subCategory));

        List<SubCategory> subCategory1 = subCategorieService.getAllSubCategorie();

        assertEquals(subCategory1.size(), 1);
        assertEquals(subCategory1.get(0).getId(), subCategory.getId());
        assertEquals(subCategory1.get(0).getName(), subCategory.getName());
        assertEquals(subCategory1.get(0).getDescription(), subCategory.getDescription());
        assertEquals(subCategory1.get(0).getSlug(), subCategory.getSlug());
        assertEquals(subCategory1.get(0).getCategorieId(), subCategory.getCategorieId());

        verify(subCategoryRepository, times(1)).findAll();
    }

}
