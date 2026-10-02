package com.client.productionreview.service;

import com.client.productionreview.dtos.admin.StatsDTO;
import com.client.productionreview.dtos.admin.TopProductDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.impl.StatsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SubCategoryRepository subCategoryRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;

    private StatsServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-02T15:00:00Z"), ZONE);
        Constructor<StatsServiceImpl> constructor = StatsServiceImpl.class.getDeclaredConstructor(ProductRepository.class,
                CategoryRepository.class, SubCategoryRepository.class, ReviewRepository.class, UserRepository.class, Clock.class);
        constructor.setAccessible(true);
        service = constructor.newInstance(productRepository, categoryRepository, subCategoryRepository, reviewRepository,
                userRepository, clock);
    }

    private ReviewRepository.CreatedNote created(Instant at, long note) {
        return new ReviewRepository.CreatedNote() {
            @Override
            public Instant getCreatedAt() {
                return at;
            }

            @Override
            public Long getNote() {
                return note;
            }
        };
    }

    private ReviewRepository.NoteCount noteCount(long note, long total) {
        return new ReviewRepository.NoteCount() {
            @Override
            public Long getNote() {
                return note;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    @Test
    void stats_fillsEveryDayWithoutGaps() {
        when(productRepository.count()).thenReturn(3L);
        when(categoryRepository.count()).thenReturn(2L);
        when(subCategoryRepository.count()).thenReturn(3L);
        when(userRepository.count()).thenReturn(2L);
        when(reviewRepository.countByStatus(ReviewStatus.VISIBLE)).thenReturn(3L);
        when(reviewRepository.countByStatus(ReviewStatus.HIDDEN)).thenReturn(1L);
        when(reviewRepository.averageNote(ReviewStatus.VISIBLE)).thenReturn(4.3333);
        when(reviewRepository.countByNote(ReviewStatus.VISIBLE)).thenReturn(List.of(noteCount(5, 2), noteCount(3, 1)));
        when(reviewRepository.findCreatedSince(eq(ReviewStatus.VISIBLE), eq(LocalDate.of(2026, 9, 26).atStartOfDay(ZONE).toInstant())))
                .thenReturn(List.of(created(Instant.parse("2026-10-02T12:00:00Z"), 5),
                        created(Instant.parse("2026-10-02T13:00:00Z"), 4),
                        created(Instant.parse("2026-09-28T13:00:00Z"), 3)));
        when(userRepository.findCreatedSince(LocalDate.of(2026, 9, 26).atStartOfDay(ZONE).toInstant()))
                .thenReturn(List.of(Instant.parse("2026-09-26T11:00:00Z")));
        when(reviewRepository.topProducts(eq(ReviewStatus.VISIBLE), any()))
                .thenReturn(List.of(new TopProductDTO(1L, "Smartphone X", "smartphone-x", 2L, 4.0)));
        when(reviewRepository.topCategories(eq(ReviewStatus.VISIBLE), any())).thenReturn(List.of());

        StatsDTO stats = service.getStats(7);

        assertEquals(3, stats.getTotals().getProducts());
        assertEquals(3, stats.getTotals().getReviews());
        assertEquals(1, stats.getTotals().getHiddenReviews());
        assertEquals(4.3, stats.getAverageNote());
        assertEquals(List.of("1", "2", "3", "4", "5"), List.copyOf(stats.getRatingDistribution().keySet()));
        assertEquals(2L, stats.getRatingDistribution().get("5"));
        assertEquals(0L, stats.getRatingDistribution().get("4"));

        // 7 dias corridos, de 26/09 a 02/10, sem buracos
        assertEquals(7, stats.getReviewsPerDay().size());
        assertEquals(LocalDate.of(2026, 9, 26), stats.getReviewsPerDay().get(0).getDate());
        assertEquals(LocalDate.of(2026, 10, 2), stats.getReviewsPerDay().get(6).getDate());
        assertEquals(0, stats.getReviewsPerDay().get(0).getCount());
        assertNull(stats.getReviewsPerDay().get(0).getAverageNote());
        assertEquals(1, stats.getReviewsPerDay().get(2).getCount());
        assertEquals(2, stats.getReviewsPerDay().get(6).getCount());
        assertEquals(4.5, stats.getReviewsPerDay().get(6).getAverageNote());

        assertEquals(7, stats.getUsersPerDay().size());
        assertEquals(1, stats.getUsersPerDay().get(0).getCount());
        assertEquals(0, stats.getUsersPerDay().get(6).getCount());

        assertEquals("Smartphone X", stats.getTopProducts().get(0).getName());
    }

    @Test
    void stats_withoutReviews_hasNullAverage() {
        when(reviewRepository.averageNote(ReviewStatus.VISIBLE)).thenReturn(null);

        StatsDTO stats = service.getStats(30);

        assertNull(stats.getAverageNote());
        assertEquals(30, stats.getReviewsPerDay().size());
        assertTrue(stats.getRatingDistribution().values().stream().allMatch(v -> v == 0L));
    }

    @Test
    void stats_groupsDaysInSaoPauloTimezone() {
        // 02:30 UTC de 02/10 ainda é 01/10 (23:30) em São Paulo
        when(reviewRepository.findCreatedSince(eq(ReviewStatus.VISIBLE), any()))
                .thenReturn(List.of(created(Instant.parse("2026-10-02T02:30:00Z"), 5)));

        StatsDTO stats = service.getStats(7);

        assertEquals(LocalDate.of(2026, 10, 1), stats.getReviewsPerDay().get(5).getDate());
        assertEquals(1, stats.getReviewsPerDay().get(5).getCount());
        assertEquals(0, stats.getReviewsPerDay().get(6).getCount());
    }

    @Test
    void stats_daysOutOfRange_isBadRequest() {
        assertThrows(BadRequestException.class, () -> service.getStats(6));
        assertThrows(BadRequestException.class, () -> service.getStats(366));
    }
}
