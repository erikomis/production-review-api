package com.client.productionreview.repository;


import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles(profiles = "test")
public class CategorieRepositoryTest {


    @Autowired
    private CategoryRepository categorieRepository;


    @BeforeEach
    public void loadCategorie() {
        categorieRepository.save(Category.builder().name("categorie1").description("description1").slug("slug1").build());
        categorieRepository.save(Category.builder().name("categorie2").description("description2").slug("slug2").build());
    }


    @Test
    public void testFindByName(){

        assertEquals("categorie1", categorieRepository.findByName("categorie1").get().getName());
        assertEquals("categorie2", categorieRepository.findByName("categorie2").get().getName());
        assertTrue(categorieRepository.findByName("missing").isEmpty());

    }

}
