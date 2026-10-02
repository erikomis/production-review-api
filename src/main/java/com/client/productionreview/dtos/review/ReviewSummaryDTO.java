package com.client.productionreview.dtos.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryDTO {

    private Long productId;

    private Long totalReviews;

    /** Média das notas arredondada para uma casa decimal; 0 quando não há reviews. */
    private Double averageNote;
}
