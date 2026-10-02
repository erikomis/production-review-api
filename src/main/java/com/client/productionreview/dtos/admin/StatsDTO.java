package com.client.productionreview.dtos.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsDTO {

    private Totals totals;

    /** Média geral das reviews visíveis (1 casa); null sem reviews. */
    private Double averageNote;

    private Map<String, Long> ratingDistribution;

    private List<ReviewsPerDay> reviewsPerDay;

    private List<UsersPerDay> usersPerDay;

    private List<TopProductDTO> topProducts;

    private List<TopCategoryDTO> topCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private long products;
        private long categories;
        private long subCategories;
        /** Reviews visíveis. */
        private long reviews;
        private long hiddenReviews;
        private long users;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewsPerDay {
        private LocalDate date;
        private long count;
        private Double averageNote;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsersPerDay {
        private LocalDate date;
        private long count;
    }
}
