package com.client.productionreview.service;

import com.client.productionreview.dtos.admin.DeduplicationResultDTO;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.ProductFollow;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductFollowRepository;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.impl.CatalogDeduplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Deduplicação contra o H2 (a detecção depende da consulta de duplicados). */
@DataJpaTest
@ActiveProfiles(profiles = "test")
class CatalogDeduplicationServiceTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private ProductFollowRepository productFollowRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;

    private StorageService storageService;
    private DomainEventPublisher eventPublisher;
    private CatalogDeduplicationServiceImpl service;
    private SubCategory coffees;
    private SubCategory milks;
    private User user;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        eventPublisher = mock(DomainEventPublisher.class);
        service = new CatalogDeduplicationServiceImpl(productRepository, productImageRepository, productFollowRepository,
                reviewRepository, storageService, eventPublisher, "bucket");

        Category drinks = categoryRepository.save(Category.builder().name("Bebidas").description("d").slug("bebidas").build());
        coffees = subCategoryRepository.save(SubCategory.builder().name("Cafés").description("d").slug("cafes")
                .categorieId(drinks.getId()).build());
        milks = subCategoryRepository.save(SubCategory.builder().name("Leites").description("d").slug("leites")
                .categorieId(drinks.getId()).build());
        user = userRepository.save(User.builder().name("U").email("u@mail.com").username("u").password("p").active(true).build());
    }

    private Product product(String name, String slug, SubCategory sub) {
        return productRepository.save(Product.builder().name(name).description("d").slug(slug).subCategorieId(sub.getId()).build());
    }

    private void image(Product product, String filename) {
        ProductImage image = new ProductImage();
        image.setProductId(product.getId());
        image.setUrlImage("https://img/" + filename);
        image.setType("image/jpeg");
        image.setFilename(filename);
        productImageRepository.save(image);
    }

    @Test
    void deduplicate_keepsOldestAndRemovesDuplicatesWithoutReviews() {
        Product pilao = product("Café Pilão", "cafe-pilao-1", coffees);
        Product pilaoDup = product("CAFE  PILAO", "cafe-pilao-2", coffees);
        Product pilaoDup2 = product("café pilão", "cafe-pilao-3", coffees);
        image(pilaoDup, "external:off:2");
        image(pilaoDup2, "product/9/abc.jpg");
        productFollowRepository.save(new ProductFollow(pilaoDup.getId(), user.getId()));

        Product milk = product("Leite Integral", "leite-1", milks);
        Product milkDup = product("Leite integral", "leite-2", milks);
        // mesmo nome em outra subcategoria não é duplicado
        Product otherSub = product("Café Pilão", "cafe-pilao-leite", milks);
        Product unique = product("Café Melitta", "cafe-melitta", coffees);

        DeduplicationResultDTO result = service.deduplicate(User.builder().id(1L).name("Admin").build());

        assertEquals(2, result.getGroups());
        assertEquals(3, result.getRemoved());
        assertEquals(java.util.Set.of(pilao.getId(), milk.getId()), java.util.Set.copyOf(result.getKeptIds()));
        assertTrue(result.getRemovedIds().containsAll(List.of(pilaoDup.getId(), pilaoDup2.getId(), milkDup.getId())));
        assertTrue(productRepository.existsById(pilao.getId()));
        assertTrue(productRepository.existsById(otherSub.getId()));
        assertTrue(productRepository.existsById(unique.getId()));
        assertFalse(productRepository.existsById(pilaoDup.getId()));
        assertEquals(0, productImageRepository.count());
        assertEquals(0, productFollowRepository.count());
        // imagem externa só sai do banco; a do storage é apagada lá também
        verify(storageService).deleteFile("bucket", "product/9/abc.jpg");
        verify(storageService, never()).deleteFile(anyString(), startsWith("external:"));
        verify(eventPublisher).publish(eq(EventType.CATALOG_DEDUPLICATED), isNull(), contains("removeu 3 produtos"), any());
    }

    @Test
    void deduplicate_neverRemovesProductsWithReviews() {
        Product first = product("Café Pilão", "cafe-1", coffees);
        Product reviewed = product("Cafe Pilao", "cafe-2", coffees);
        reviewRepository.save(Review.builder().title("t").description("d").note(5L).productId(reviewed.getId())
                .userId(user.getId()).build());

        DeduplicationResultDTO result = service.deduplicate(null);

        assertEquals(1, result.getGroups());
        assertEquals(0, result.getRemoved());
        assertEquals(List.of(first.getId()), result.getKeptIds());
        assertTrue(result.getRemovedIds().isEmpty());
        assertTrue(productRepository.existsById(reviewed.getId()));
    }

    @Test
    void deduplicate_withoutDuplicates_returnsZero() {
        product("Café Pilão", "cafe-1", coffees);

        DeduplicationResultDTO result = service.deduplicate(null);

        assertEquals(0, result.getGroups());
        assertEquals(0, result.getRemoved());
        verifyNoInteractions(storageService);
    }
}
