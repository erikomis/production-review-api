package com.client.productionreview.repository;

import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;


import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles(profiles = "test")
public class SubCategorieRepositoryTest {

    @Autowired
    private SubCategoryRepository subCategorieRepository;

    @Autowired
    private CategoryRepository categorieRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category categorie1;
    private Category categorie2;

    @BeforeEach
    public void setUp() {
        categorie1 = categorieRepository.save(Category.builder().name("categorie1").description("description1").slug("slug1").build());
        categorie2 = categorieRepository.save(Category.builder().name("categorie2").description("description2").slug("slug2").build());

        saveSubCategory("sub1", "sub-slug1", categorie1.getId());
        saveSubCategory("sub2", "sub-slug2", categorie1.getId());
    }

    private SubCategory saveSubCategory(String name, String slug, Long categoryId) {
        SubCategory subCategory = new SubCategory();
        subCategory.setName(name);
        subCategory.setDescription("description");
        subCategory.setSlug(slug);
        subCategory.setCategorieId(categoryId);
        return subCategorieRepository.saveAndFlush(subCategory);
    }

    @Test
    public void testFindByName(){

        assertEquals("sub1", subCategorieRepository.findByName("sub1").get().getName());
        assertEquals("sub2", subCategorieRepository.findByName("sub2").get().getName());

    }

    @Test
    public void categorieIdIsPersisted() {
        // antes da correção category_id era insertable=false e era gravado como NULL
        entityManager.clear();
        SubCategory loaded = subCategorieRepository.findByName("sub1").orElseThrow();

        assertEquals(categorie1.getId(), loaded.getCategorieId());
        assertNotNull(loaded.getCategory());
        assertEquals("categorie1", loaded.getCategory().getName());
    }

    @Test
    public void existsByCategorieId_worksWithMultipleSubCategories() {
        assertTrue(subCategorieRepository.existsByCategorieId(categorie1.getId()));
        assertFalse(subCategorieRepository.existsByCategorieId(categorie2.getId()));
    }

}
