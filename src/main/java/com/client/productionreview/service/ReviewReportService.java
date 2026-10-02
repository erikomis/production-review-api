package com.client.productionreview.service;

import com.client.productionreview.dtos.review.ReviewReportDTO;
import com.client.productionreview.dtos.review.ReviewReportRequestDTO;
import com.client.productionreview.model.jpa.User;

import java.util.List;

public interface ReviewReportService {

    /** 409 se o usuário já denunciou; 400 na própria review; 404 se oculta ou inexistente. */
    ReviewReportDTO report(Long reviewId, ReviewReportRequestDTO request, User user);

    /** Denúncias da review, mais recentes primeiro (ADMIN). */
    List<ReviewReportDTO> listReports(Long reviewId);

    /** Descarta todas as denúncias da review (ADMIN). */
    void dismissReports(Long reviewId, User admin);
}
