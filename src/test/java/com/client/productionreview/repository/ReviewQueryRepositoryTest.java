package com.client.productionreview.repository;

import com.client.productionreview.dtos.admin.TopCategoryDTO;
import com.client.productionreview.dtos.admin.TopProductDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import com.client.productionreview.dtos.review.ReviewSort;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewHelpful;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewHelpfulRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles(profiles = "test")
class ReviewQueryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewHelpfulRepository reviewHelpfulRepository;
    @Autowired
    private UserRepository userRepository;

    private Product phone;
    private Product laptop;
    private User author;
    private User reader;
    private User admin;
    private Review good;
    private Review bad;
    private Review hidden;
    private Review other;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder().name("Eletrônicos").description("d").slug("eletronicos").build());
        SubCategory sub = subCategoryRepository.save(SubCategory.builder().name("Celulares").description("d").slug("celulares")
                .categorieId(category.getId()).build());
        phone = productRepository.save(Product.builder().name("Smartphone X").description("d").slug("smartphone-x").subCategorieId(sub.getId()).build());
        laptop = productRepository.save(Product.builder().name("Notebook").description("d").slug("notebook").subCategorieId(sub.getId()).build());

        author = user("Autor", "autor");
        reader = user("Leitor", "leitor");
        admin = user("Admin", "admin");

        good = review(phone, author, 5, "Excelente aparelho", ReviewStatus.VISIBLE);
        bad = review(phone, reader, 2, "Bateria fraca", ReviewStatus.VISIBLE);
        hidden = review(phone, reader, 1, "Spam total", ReviewStatus.HIDDEN);
        hidden.setModeratedBy(admin.getId());
        hidden.setModerationReason("spam");
        hidden.setModeratedAt(LocalDateTime.now());
        reviewRepository.save(hidden);
        other = review(laptop, author, 4, "Bom notebook", ReviewStatus.VISIBLE);

        reviewHelpfulRepository.save(new ReviewHelpful(bad.getId(), author.getId()));
        reviewHelpfulRepository.save(new ReviewHelpful(bad.getId(), admin.getId()));
        reviewHelpfulRepository.save(new ReviewHelpful(good.getId(), reader.getId()));
    }

    private User user(String name, String username) {
        return userRepository.save(User.builder().name(name).email(username + "@mail.com").username(username)
                .password("p").active(true).build());
    }

    private Review review(Product product, User user, long note, String title, ReviewStatus status) {
        return reviewRepository.save(Review.builder().title(title).description("descrição de " + title).note(note)
                .productId(product.getId()).userId(user.getId()).status(status).build());
    }

    private List<Long> ids(Page<ReviewResponseDTO> page) {
        return page.getContent().stream().map(ReviewResponseDTO::getId).toList();
    }

    @Test
    void visibleOfProduct_fillsNamesSlugAndHelpfulCount() {
        Page<ReviewResponseDTO> page = reviewRepository.searchDetails(ReviewSearch.builder().productId(phone.getId())
                .status(ReviewStatus.VISIBLE).sort(ReviewSort.recent).build(), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertFalse(ids(page).contains(hidden.getId()));

        ReviewResponseDTO badDto = page.getContent().stream().filter(r -> r.getId().equals(bad.getId())).findFirst().orElseThrow();
        assertEquals(2L, badDto.getHelpfulCount());
        assertEquals("Smartphone X", badDto.getProductName());
        assertEquals("smartphone-x", badDto.getProductSlug());
        assertEquals("Leitor", badDto.getUserName());
        assertEquals(ReviewStatus.VISIBLE, badDto.getStatus());
    }

    @Test
    void sorts_helpfulHighestLowest() {
        ReviewSearch base = ReviewSearch.builder().productId(phone.getId()).status(ReviewStatus.VISIBLE).build();

        assertEquals(List.of(bad.getId(), good.getId()),
                ids(reviewRepository.searchDetails(base.toBuilder().sort(ReviewSort.helpful).build(), PageRequest.of(0, 10))));
        assertEquals(List.of(good.getId(), bad.getId()),
                ids(reviewRepository.searchDetails(base.toBuilder().sort(ReviewSort.highest).build(), PageRequest.of(0, 10))));
        assertEquals(List.of(bad.getId(), good.getId()),
                ids(reviewRepository.searchDetails(base.toBuilder().sort(ReviewSort.lowest).build(), PageRequest.of(0, 10))));
    }

    @Test
    void recentAndOldest_orderByCreation() {
        List<Long> recent = ids(reviewRepository.searchDetails(ReviewSearch.builder().sort(ReviewSort.recent).build(), PageRequest.of(0, 10)));
        List<Long> oldest = ids(reviewRepository.searchDetails(ReviewSearch.builder().sort(ReviewSort.oldest).build(), PageRequest.of(0, 10)));

        assertEquals(other.getId(), recent.get(0));
        assertEquals(good.getId(), oldest.get(0));
    }

    @Test
    void filters_noteSearchUserAndStatus_forModeration() {
        assertEquals(List.of(bad.getId()), ids(reviewRepository.searchDetails(
                ReviewSearch.builder().note(2L).build(), PageRequest.of(0, 10))));
        assertEquals(List.of(other.getId()), ids(reviewRepository.searchDetails(
                ReviewSearch.builder().search("NOTEBOOK").build(), PageRequest.of(0, 10))));
        // a busca também olha a descrição
        assertEquals(List.of(hidden.getId()), ids(reviewRepository.searchDetails(
                ReviewSearch.builder().search("de spam").build(), PageRequest.of(0, 10))));

        Page<ReviewResponseDTO> hiddenPage = reviewRepository.searchDetails(
                ReviewSearch.builder().status(ReviewStatus.HIDDEN).build(), PageRequest.of(0, 10));
        assertEquals(1, hiddenPage.getTotalElements());
        assertEquals("Admin", hiddenPage.getContent().get(0).getModeratedByName());
        assertEquals("spam", hiddenPage.getContent().get(0).getModerationReason());

        // "minhas reviews" inclui as ocultadas
        assertEquals(2, reviewRepository.searchDetails(ReviewSearch.builder().userId(reader.getId()).build(), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void pagination_countIgnoresHelpfulJoin() {
        Page<ReviewResponseDTO> page = reviewRepository.searchDetails(ReviewSearch.builder().build(), PageRequest.of(0, 2));

        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void ratingSummaryAndDistribution_onlyVisible() {
        var summary = reviewRepository.getRatingSummary(phone.getId());
        assertEquals(2L, summary.getTotalReviews());
        assertEquals(3.5, summary.getAverageNote(), 0.001);

        Map<Long, Long> distribution = reviewRepository.countByNoteForProduct(phone.getId()).stream()
                .collect(Collectors.toMap(ReviewRepository.NoteCount::getNote, ReviewRepository.NoteCount::getTotal));
        assertEquals(Map.of(5L, 1L, 2L, 1L), distribution);
    }

    @Test
    void statsQueries() {
        assertEquals(3, reviewRepository.countByStatus(ReviewStatus.VISIBLE));
        assertEquals(1, reviewRepository.countByStatus(ReviewStatus.HIDDEN));
        assertEquals(3.667, reviewRepository.averageNote(ReviewStatus.VISIBLE), 0.001);
        assertEquals(3, reviewRepository.findCreatedSince(ReviewStatus.VISIBLE, LocalDateTime.now().minusDays(1)).size());

        List<TopProductDTO> top = reviewRepository.topProducts(ReviewStatus.VISIBLE, PageRequest.of(0, 5));
        assertEquals("Smartphone X", top.get(0).getName());
        assertEquals(2, top.get(0).getTotalReviews());
        assertEquals(3.5, top.get(0).getAverageNote());
        assertEquals("smartphone-x", top.get(0).getSlug());

        List<TopCategoryDTO> categories = reviewRepository.topCategories(ReviewStatus.VISIBLE, PageRequest.of(0, 5));
        assertEquals(1, categories.size());
        assertEquals(3, categories.get(0).getTotalReviews());
        assertEquals(3.7, categories.get(0).getAverageNote());

        Map<Long, Long> perUser = reviewRepository.countByUsers(List.of(author.getId(), reader.getId(), admin.getId())).stream()
                .collect(Collectors.toMap(ReviewRepository.UserCount::getUserId, ReviewRepository.UserCount::getTotal));
        assertEquals(Map.of(author.getId(), 2L, reader.getId(), 2L), perUser);
    }

    @Test
    void helpfulRepository_marksAndUnmarks() {
        assertTrue(reviewHelpfulRepository.existsByReviewIdAndUserId(bad.getId(), author.getId()));
        assertEquals(2, reviewHelpfulRepository.countByReviewId(bad.getId()));
        assertEquals(List.of(bad.getId()), reviewHelpfulRepository.findReviewIdsMarkedBy(author.getId(),
                List.of(good.getId(), bad.getId(), other.getId())));

        reviewHelpfulRepository.deleteMark(bad.getId(), author.getId());
        assertEquals(1, reviewHelpfulRepository.countByReviewId(bad.getId()));

        reviewHelpfulRepository.deleteByReview(bad.getId());
        assertEquals(0, reviewHelpfulRepository.countByReviewId(bad.getId()));
    }
}
