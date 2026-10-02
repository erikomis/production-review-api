package com.client.productionreview.repository;

import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.ProductFollow;
import com.client.productionreview.model.jpa.ReportReason;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewHelpful;
import com.client.productionreview.model.jpa.ReviewImage;
import com.client.productionreview.model.jpa.ReviewReport;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Denúncias, fotos, resposta oficial, seguidores e perfil público contra o H2. */
@DataJpaTest
@ActiveProfiles(profiles = "test")
class ReviewReportAndFollowRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewReportRepository reviewReportRepository;
    @Autowired
    private ReviewImageRepository reviewImageRepository;
    @Autowired
    private ReviewHelpfulRepository reviewHelpfulRepository;
    @Autowired
    private ProductFollowRepository productFollowRepository;
    @Autowired
    private UserRepository userRepository;

    private Product cafe;
    private Product leite;
    private User maria;
    private User ana;
    private User admin;
    private Review good;
    private Review spam;

    @BeforeEach
    void setUp() {
        Category drinks = categoryRepository.save(Category.builder().name("Bebidas").description("d").slug("bebidas").build());
        SubCategory sub = subCategoryRepository.save(SubCategory.builder().name("Cafés").description("d").slug("cafes")
                .categorieId(drinks.getId()).build());
        cafe = productRepository.save(Product.builder().name("Café").description("d").slug("cafe").subCategorieId(sub.getId()).build());
        leite = productRepository.save(Product.builder().name("Leite").description("d").slug("leite").subCategorieId(sub.getId()).build());
        maria = user("Maria", "maria");
        ana = user("Ana", "ana");
        admin = user("Admin", "admin");

        good = reviewRepository.save(Review.builder().title("Ótimo").description("bom").note(5L).productId(cafe.getId())
                .userId(maria.getId()).status(ReviewStatus.VISIBLE).replyText("Obrigado!").replyAuthorId(admin.getId())
                .repliedAt(Instant.parse("2026-10-02T03:00:00Z")).build());
        spam = reviewRepository.save(Review.builder().title("Compre já").description("spam").note(1L).productId(cafe.getId())
                .userId(ana.getId()).status(ReviewStatus.VISIBLE).build());
    }

    private User user(String name, String username) {
        return userRepository.save(User.builder().name(name).email(username + "@mail.com").username(username)
                .password("p").active(true).build());
    }

    private ReviewReport report(Review review, User user, ReportReason reason) {
        return reviewReportRepository.saveAndFlush(ReviewReport.builder().reviewId(review.getId()).userId(user.getId())
                .reason(reason).details("detalhes").build());
    }

    // ---------- denúncias ----------

    @Test
    void report_isUniquePerUserAndReview() {
        report(spam, maria, ReportReason.SPAM);

        assertTrue(reviewReportRepository.existsByReviewIdAndUserId(spam.getId(), maria.getId()));
        assertThrows(DataIntegrityViolationException.class, () -> report(spam, maria, ReportReason.OTHER));
    }

    @Test
    void reportViews_countsAndReportedByMe() {
        report(spam, maria, ReportReason.SPAM);
        report(spam, admin, ReportReason.OFFENSIVE);
        report(good, ana, ReportReason.FALSE_INFORMATION);

        Map<Long, Long> counts = reviewReportRepository.countByReviews(List.of(spam.getId(), good.getId())).stream()
                .collect(Collectors.toMap(ReviewReportRepository.ReviewCount::getReviewId, ReviewReportRepository.ReviewCount::getTotal));
        assertEquals(2L, counts.get(spam.getId()));
        assertEquals(1L, counts.get(good.getId()));

        assertEquals(List.of(spam.getId()), reviewReportRepository.findReviewIdsReportedBy(maria.getId(),
                List.of(spam.getId(), good.getId())));

        List<ReviewReportRepository.ReportView> views = reviewReportRepository.findViewsByReview(spam.getId());
        assertEquals(2, views.size());
        assertTrue(views.stream().map(ReviewReportRepository.ReportView::getReporterName).toList().containsAll(List.of("Maria", "Admin")));
        assertNotNull(views.get(0).getCreatedAt());
    }

    @Test
    void searchDetails_reportedFilter() {
        report(spam, maria, ReportReason.SPAM);

        List<Long> reported = reviewRepository.searchDetails(ReviewSearch.builder().reported(true).build(), PageRequest.of(0, 10))
                .getContent().stream().map(ReviewResponseDTO::getId).toList();
        List<Long> notReported = reviewRepository.searchDetails(ReviewSearch.builder().reported(false).build(), PageRequest.of(0, 10))
                .getContent().stream().map(ReviewResponseDTO::getId).toList();

        assertEquals(List.of(spam.getId()), reported);
        assertEquals(List.of(good.getId()), notReported);
        assertEquals(1, reviewRepository.searchDetails(ReviewSearch.builder().reported(true).build(), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void deleteReports_byReviewAndByReviews() {
        report(spam, maria, ReportReason.SPAM);
        report(spam, admin, ReportReason.SPAM);
        report(good, ana, ReportReason.SPAM);

        assertEquals(2, reviewReportRepository.deleteByReview(spam.getId()));
        assertEquals(1, reviewReportRepository.deleteByReviews(List.of(good.getId())));
        assertEquals(0, reviewReportRepository.count());
    }

    // ---------- projeção: resposta oficial, username, fotos ----------

    @Test
    void searchDetails_includesReplyAndUsername() {
        ReviewResponseDTO dto = reviewRepository.searchDetails(ReviewSearch.builder().reviewId(good.getId()).build(),
                PageRequest.of(0, 1)).getContent().get(0);

        assertEquals("maria", dto.getUserUsername());
        assertEquals("Obrigado!", dto.getReply().getText());
        assertEquals("Admin", dto.getReply().getAuthorName());
        assertEquals(Instant.parse("2026-10-02T03:00:00Z"), dto.getReply().getRepliedAt());

        ReviewResponseDTO noReply = reviewRepository.searchDetails(ReviewSearch.builder().reviewId(spam.getId()).build(),
                PageRequest.of(0, 1)).getContent().get(0);
        assertNull(noReply.getReply());
        assertTrue(noReply.getImages().isEmpty());
    }

    @Test
    void reviewImages_byReviewsInOrder() {
        reviewImageRepository.save(ReviewImage.builder().reviewId(good.getId()).objectKey("reviews/1/a.jpg").contentType("image/jpeg").sizeBytes(10L).build());
        reviewImageRepository.save(ReviewImage.builder().reviewId(good.getId()).objectKey("reviews/1/b.jpg").contentType("image/jpeg").sizeBytes(10L).build());
        reviewImageRepository.save(ReviewImage.builder().reviewId(spam.getId()).objectKey("reviews/2/c.jpg").contentType("image/jpeg").sizeBytes(10L).build());

        assertEquals(2, reviewImageRepository.countByReviewId(good.getId()));
        assertEquals(List.of("reviews/1/a.jpg", "reviews/1/b.jpg", "reviews/2/c.jpg"),
                reviewImageRepository.findByReviewIdInOrderByIdAsc(List.of(good.getId(), spam.getId())).stream()
                        .map(ReviewImage::getObjectKey).toList());
        assertEquals(2, reviewImageRepository.deleteByReview(good.getId()));
        assertThrows(DataIntegrityViolationException.class, () -> reviewImageRepository.saveAndFlush(ReviewImage.builder()
                .reviewId(spam.getId()).objectKey("reviews/2/c.jpg").contentType("image/jpeg").sizeBytes(1L).build()));
    }

    // ---------- seguir produto ----------

    @Test
    void follow_countsAndFollowers() {
        productFollowRepository.save(new ProductFollow(cafe.getId(), maria.getId()));
        productFollowRepository.save(new ProductFollow(cafe.getId(), ana.getId()));
        productFollowRepository.save(new ProductFollow(leite.getId(), maria.getId()));

        assertEquals(2, productFollowRepository.countByProductId(cafe.getId()));
        assertTrue(productFollowRepository.existsByProductIdAndUserId(cafe.getId(), ana.getId()));
        assertEquals(List.of(maria.getId(), ana.getId()).stream().sorted().toList(),
                productFollowRepository.findFollowerIds(cafe.getId()).stream().sorted().toList());

        assertEquals(1, productFollowRepository.deleteFollow(cafe.getId(), ana.getId()));
        assertEquals(0, productFollowRepository.deleteFollow(cafe.getId(), ana.getId()));
        assertEquals(1, productFollowRepository.countByProductId(cafe.getId()));
    }

    @Test
    void followedByFilter_listsFollowedProductsWithRatings() {
        productFollowRepository.save(new ProductFollow(cafe.getId(), maria.getId()));

        List<ProductSummaryDTO> following = productRepository.findSummaries(ProductFilter.followedBy(maria.getId()),
                PageRequest.of(0, 10)).getContent();

        assertEquals(1, following.size());
        assertEquals("Café", following.get(0).getName());
        assertEquals(2, following.get(0).getTotalReviews());
        assertEquals(0, productRepository.findSummaries(ProductFilter.followedBy(ana.getId()), PageRequest.of(0, 10))
                .getTotalElements());
    }

    // ---------- perfil público ----------

    @Test
    void profileQueries_countOnlyVisibleReviews() {
        reviewRepository.save(Review.builder().title("Oculta").description("x").note(1L).productId(leite.getId())
                .userId(maria.getId()).status(ReviewStatus.HIDDEN).build());
        reviewHelpfulRepository.save(new ReviewHelpful(good.getId(), ana.getId()));
        reviewHelpfulRepository.save(new ReviewHelpful(good.getId(), admin.getId()));

        ReviewRepository.RatingSummary summary = reviewRepository.getUserRatingSummary(maria.getId());

        assertEquals(1L, summary.getTotalReviews());
        assertEquals(5.0, summary.getAverageNote());
        assertEquals(2L, reviewRepository.countHelpfulReceived(maria.getId()));
        assertEquals(0L, reviewRepository.countHelpfulReceived(ana.getId()));
        assertTrue(userRepository.findByUsername("maria").isPresent());
    }
}
