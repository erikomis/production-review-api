package com.client.productionreview.repository;


import com.client.productionreview.model.Category;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@WebMvcTest(CategoryRepository.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CategorieRepositoryTest {


    @Autowired
    private CategoryRepository categorieRepository;


    @BeforeAll
    public  void loadCategorie(){

        List<Category> categories = new ArrayList<>();

        Category categorie = new Category();
        categorie.setName("categorie1");
        categorie.setDescription("description1");
        categorie.setSlug("slug1");
        categorie.setCreatedAt(LocalDateTime.now());
        categorie.setSubCategoriaId(null);
        categories.add(categorie);
        Category categorie2 = new Category();
        categorie2.setName("categorie2");
        categorie2.setDescription("description2");
        categorie2.setSlug("slug2");
        categorie2.setCreatedAt(LocalDateTime.now());


        categories.add(categorie2);

        categorieRepository.saveAll(categories);
    }


    @AfterAll
    public void deleteCategorie(){
        categorieRepository.deleteAll();
    }


    @Test
    public void testFindByName(){

        assertEquals("categorie1", categorieRepository.findByName("categorie1").get().getName());
        assertEquals("categorie2", categorieRepository.findByName("categorie2").get().getName());

    }

}
