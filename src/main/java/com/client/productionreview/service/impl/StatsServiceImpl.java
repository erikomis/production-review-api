package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.admin.StatsDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.StatsService;
import com.client.productionreview.utils.RatingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsServiceImpl implements StatsService {

    static final int MIN_DAYS = 7;
    static final int MAX_DAYS = 365;
    static final int TOP_LIMIT = 5;

    /** As séries por dia seguem o calendário de quem usa o painel, não o UTC do banco. */
    public static final ZoneId STATS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public StatsServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository,
                            SubCategoryRepository subCategoryRepository, ReviewRepository reviewRepository,
                            UserRepository userRepository) {
        this(productRepository, categoryRepository, subCategoryRepository, reviewRepository, userRepository,
                Clock.system(STATS_ZONE));
    }

    StatsServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository,
                     SubCategoryRepository subCategoryRepository, ReviewRepository reviewRepository,
                     UserRepository userRepository, Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    public StatsDTO getStats(int days) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new BadRequestException("days deve estar entre " + MIN_DAYS + " e " + MAX_DAYS);
        }

        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant from = firstDay.atStartOfDay(zone).toInstant();

        StatsDTO.Totals totals = StatsDTO.Totals.builder()
                .products(productRepository.count())
                .categories(categoryRepository.count())
                .subCategories(subCategoryRepository.count())
                .reviews(reviewRepository.countByStatus(ReviewStatus.VISIBLE))
                .hiddenReviews(reviewRepository.countByStatus(ReviewStatus.HIDDEN))
                .users(userRepository.count())
                .build();

        Map<String, Long> distribution = RatingUtils.emptyDistribution();
        reviewRepository.countByNote(ReviewStatus.VISIBLE).stream()
                .filter(count -> count.getNote() != null && distribution.containsKey(String.valueOf(count.getNote())))
                .forEach(count -> distribution.put(String.valueOf(count.getNote()), count.getTotal()));

        return StatsDTO.builder()
                .totals(totals)
                .averageNote(RatingUtils.round(reviewRepository.averageNote(ReviewStatus.VISIBLE)))
                .ratingDistribution(distribution)
                .reviewsPerDay(reviewsPerDay(firstDay, today, zone, reviewRepository.findCreatedSince(ReviewStatus.VISIBLE, from)))
                .usersPerDay(usersPerDay(firstDay, today, zone, userRepository.findCreatedSince(from)))
                .topProducts(reviewRepository.topProducts(ReviewStatus.VISIBLE, PageRequest.of(0, TOP_LIMIT)))
                .topCategories(reviewRepository.topCategories(ReviewStatus.VISIBLE, PageRequest.of(0, TOP_LIMIT)))
                .build();
    }

    private List<StatsDTO.ReviewsPerDay> reviewsPerDay(LocalDate firstDay, LocalDate today, ZoneId zone,
                                                       List<ReviewRepository.CreatedNote> reviews) {
        Map<LocalDate, long[]> byDay = new HashMap<>();
        for (ReviewRepository.CreatedNote review : reviews) {
            if (review.getCreatedAt() == null) {
                continue;
            }
            long[] acc = byDay.computeIfAbsent(review.getCreatedAt().atZone(zone).toLocalDate(), d -> new long[2]);
            acc[0]++;
            acc[1] += review.getNote() == null ? 0 : review.getNote();
        }

        List<StatsDTO.ReviewsPerDay> result = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            long[] acc = byDay.get(day);
            if (acc == null) {
                result.add(new StatsDTO.ReviewsPerDay(day, 0, null));
            } else {
                result.add(new StatsDTO.ReviewsPerDay(day, acc[0], RatingUtils.round((double) acc[1] / acc[0])));
            }
        }
        return result;
    }

    private List<StatsDTO.UsersPerDay> usersPerDay(LocalDate firstDay, LocalDate today, ZoneId zone, List<Instant> createdAt) {
        Map<LocalDate, Long> byDay = new HashMap<>();
        createdAt.stream()
                .filter(date -> date != null)
                .forEach(date -> byDay.merge(date.atZone(zone).toLocalDate(), 1L, Long::sum));

        List<StatsDTO.UsersPerDay> result = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            result.add(new StatsDTO.UsersPerDay(day, byDay.getOrDefault(day, 0L)));
        }
        return result;
    }
}
