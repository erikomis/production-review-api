package com.client.productionreview.repositories.jpa;

import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Listagens de review com nomes, slug do produto e contagem de "útil" (fragmento do ReviewRepository). */
public interface ReviewQueryRepository {

    /** A ordenação vem de {@link ReviewSearch#sort()}; a do pageable é ignorada. */
    Page<ReviewResponseDTO> searchDetails(ReviewSearch search, Pageable pageable);
}
