package com.client.productionreview.service;

import com.client.productionreview.model.jpa.ReviewStatus;
import org.springframework.util.MultiValueMap;

/** Exportações CSV do painel (até 10.000 linhas cada). */
public interface AdminExportService {

    int MAX_ROWS = 10_000;

    byte[] reviewsCsv(ReviewStatus status, Long note, Long productId, String search, Boolean reported);

    byte[] usersCsv(String search, String role, Boolean active);

    /** Mesmos filtros de {@code GET /admin/activity}. */
    byte[] activityCsv(MultiValueMap<String, String> params);
}
