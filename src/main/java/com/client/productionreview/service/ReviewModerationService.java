package com.client.productionreview.service;

import com.client.productionreview.dtos.review.ReviewBulkModerationRequestDTO;
import com.client.productionreview.dtos.review.ReviewModerationRequestDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewModerationService {

    int EXPORT_LIMIT = 10_000;

    /**
     * Todas as reviews (qualquer status), mais recentes primeiro. {@code search} busca em título e descrição;
     * {@code reported} = true mostra só as denunciadas. Inclui {@code reportsCount}.
     */
    Page<ReviewResponseDTO> listReviews(ReviewStatus status, Long note, Long productId, String search, Boolean reported,
                                        Pageable pageable);

    default Page<ReviewResponseDTO> listReviews(ReviewStatus status, Long note, Long productId, String search, Pageable pageable) {
        return listReviews(status, note, productId, search, null, pageable);
    }

    /** Oculta (motivo obrigatório) ou restaura uma review. Ocultar resolve (apaga) as denúncias. */
    ReviewResponseDTO moderate(Long reviewId, ReviewModerationRequestDTO request, User admin);

    /** Mesmas regras do individual para até 100 reviews; ids inexistentes são ignorados. Devolve quantas mudaram. */
    int moderateBulk(ReviewBulkModerationRequestDTO request, User admin);

    /** Cria ou substitui a resposta oficial e avisa o autor. */
    ReviewResponseDTO reply(Long reviewId, String text, User admin);

    void deleteReply(Long reviewId, User admin);
}
