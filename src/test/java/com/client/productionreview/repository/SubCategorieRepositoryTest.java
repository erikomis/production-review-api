package com.client.productionreview.repository;

import com.client.productionreview.model.Category;
import com.client.productionreview.model.SubCategory;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;



import static org.junit.jupiter.api.Assertions.assertEquals;

@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@WebMvcTest(SubCategoryRepository.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubCategorieRepositoryTest {

    @Autowired
    private SubCategoryRepository subCategorieRepository;

    @Autowired
    private CategoryRepository categorieRepository;

    @BeforeEach
    public void setUp() {
        // Limpar dados antes de cada teste
        subCategorieRepository.deleteAll();
        categorieRepository.deleteAll();

        // Criar e salvar Categorias
        Category categorie1 = new Category();
        categorie1.setName("categorie1");
        categorie1.setDescription("description1");
        categorie1.setSlug("slug1");
        categorieRepository.save(categorie1);

        Category categorie2 = new Category();
        categorie2.setName("categorie2");
        categorie2.setDescription("description2");
        categorie2.setSlug("slug2");
        categorieRepository.save(categorie2);

        // Criar e salvar SubCategorias
        SubCategory subCategorie1 = new SubCategory();
        subCategorie1.setName("categorie1");
        subCategorie1.setDescription("description1");
        subCategorie1.setSlug("slug1");
        subCategorie1.setCategorieId(categorie1.getId());
        subCategorieRepository.save(subCategorie1);

        SubCategory subCategorie2 = new SubCategory();
        subCategorie2.setName("categorie2");
        subCategorie2.setDescription("description2");
        subCategorie2.setSlug("slug2");
        subCategorie2.setCategorieId(categorie2.getId());
        subCategorieRepository.save(subCategorie2);
    }

    @Test
    public void testFindByName(){

        assertEquals("categorie1", subCategorieRepository.findByName("categorie1").get().getName());
        assertEquals("categorie2", subCategorieRepository.findByName("categorie2").get().getName());

    }

}
