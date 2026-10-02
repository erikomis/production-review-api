package com.client.productionreview.dtos.review;

import com.client.productionreview.model.jpa.ReviewStatus;
import lombok.Builder;

/** Critérios das consultas de review; campos nulos não filtram. */
@Builder(toBuilder = true)
public record ReviewSearch(Long reviewId, Long productId, Long userId, ReviewStatus status, Long note, String search,
                           ReviewSort sort, Boolean reported) {

    public ReviewSort sortOrDefault() {
        return sort == null ? ReviewSort.recent : sort;
    }
}
