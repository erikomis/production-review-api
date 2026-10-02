package com.client.productionreview.service;

import com.client.productionreview.dtos.review.ReviewModerationRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewModerationService {

    /** Todas as reviews (qualquer status), mais recentes primeiro. {@code search} busca em título e descrição. */
    Page<ReviewResponseDTO> listReviews(ReviewStatus status, Long note, Long productId, String search, Pageable pageable);

    /** Oculta (motivo obrigatório) ou restaura uma review. */
    ReviewResponseDTO moderate(Long reviewId, ReviewModerationRequestDTO request, User admin);
}
