package com.client.productionreview.repository;

import com.client.productionreview.dtos.review.ReviewSearch;
import com.client.productionreview.dtos.review.ReviewSort;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles(profiles = "test")
class ProductAndReviewRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;

    private Product phone;
    private Product laptop;
    private User user;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder().name("Eletrônicos").description("d").slug("eletronicos").build());
        SubCategory sub = new SubCategory();
        sub.setName("Celulares");
        sub.setDescription("d");
        sub.setSlug("celulares");
        sub.setCategorieId(category.getId());
        sub = subCategoryRepository.save(sub);

        phone = productRepository.save(Product.builder().name("Smartphone X").description("d").slug("smartphone-x").subCategorieId(sub.getId()).build());
        laptop = productRepository.save(Product.builder().name("Notebook Pro").description("d").slug("notebook-pro").subCategorieId(sub.getId()).build());
        productRepository.save(Product.builder().name("Smartphone Y").description("d").slug("smartphone-y").subCategorieId(sub.getId()).build());

        user = userRepository.save(User.builder().name("u").email("u@mail.com").username("u").password("p").active(true).build());
    }

    private void review(Product product, long note) {
        reviewRepository.save(Review.builder().title("t").description("d").note(note).productId(product.getId()).userId(user.getId()).build());
    }

    @Test
    void search_isPartialAndCaseInsensitive_withCorrectTotal() {
        var page = productRepository.findAllByProduct("smartphone", PageRequest.of(0, 10));

        // antes: LIKE sem curingas (só nome exato) e count de todos os produtos da tabela
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(p -> p.getName().startsWith("Smartphone")));
    }

    @Test
    void ratingSummary_computesCountAndAverage() {
        review(phone, 5);
        review(phone, 4);
        review(phone, 4);
        review(laptop, 1);

        var summary = reviewRepository.getRatingSummary(phone.getId());

        assertEquals(3L, summary.getTotalReviews());
        assertEquals(4.333, summary.getAverageNote(), 0.01);
    }

    @Test
    void ratingSummary_withoutReviews() {
        var summary = reviewRepository.getRatingSummary(laptop.getId());

        assertEquals(0L, summary.getTotalReviews());
        assertNull(summary.getAverageNote());
    }

    @Test
    void findByProductIdWithDetails_paginatesAndFillsNames() {
        review(phone, 5);
        review(phone, 3);
        review(laptop, 2);

        var page = reviewRepository.searchDetails(ReviewSearch.builder().productId(phone.getId()).build(), PageRequest.of(0, 1));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("Smartphone X", page.getContent().get(0).getProductName());
        assertEquals("u", page.getContent().get(0).getUserName());
    }

    @Test
    void findAllWithDetails_sortsByCreatedAt() {
        review(phone, 5);
        review(laptop, 2);

        var page = reviewRepository.searchDetails(ReviewSearch.builder().sort(ReviewSort.recent).build(), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertEquals("Notebook Pro", page.getContent().get(0).getProductName());
    }

    @Test
    void findBySlug() {
        assertEquals("Smartphone X", productRepository.findBySlug("smartphone-x").orElseThrow().getName());
        assertTrue(productRepository.findBySlug("nope").isEmpty());
    }
}
