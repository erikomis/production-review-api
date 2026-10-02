package com.client.productionreview.dtos.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryDTO {

    private Long productId;

    private Long totalReviews;

    /** Média das notas visíveis arredondada para uma casa decimal; 0 quando não há reviews. */
    private Double averageNote;

    /** Quantidade de reviews visíveis por nota; sempre com as chaves "1" a "5". */
    private Map<String, Long> distribution;

    public ReviewSummaryDTO(Long productId, Long totalReviews, Double averageNote) {
        this(productId, totalReviews, averageNote, null);
    }
}
