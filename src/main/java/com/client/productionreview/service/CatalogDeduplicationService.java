package com.client.productionreview.service;

import com.client.productionreview.dtos.admin.DeduplicationResultDTO;
import com.client.productionreview.model.jpa.User;

public interface CatalogDeduplicationService {

    /**
     * Agrupa produtos com o mesmo nome normalizado na mesma subcategoria, mantém o mais antigo de cada grupo
     * e remove os demais que não tiverem reviews (as imagens vão junto).
     */
    DeduplicationResultDTO deduplicate(User admin);
}
