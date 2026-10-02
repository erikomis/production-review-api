package com.client.productionreview.service;

import com.client.productionreview.dtos.admin.StatsDTO;

public interface StatsService {

    /** Estatísticas do painel; {@code days} entre 7 e 365 (um item por dia, sem buracos). */
    StatsDTO getStats(int days);
}
