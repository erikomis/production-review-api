package com.client.productionreview.service;

import com.client.productionreview.dtos.importer.ImportJobDTO;
import com.client.productionreview.model.jpa.User;

import java.util.Optional;

public interface CatalogImportService {

    /** Inicia a importação em segundo plano. 409 se já houver um job rodando. */
    ImportJobDTO startOpenFoodFactsImport(int productsPerSubcategory, User startedBy);

    /** 404 se o job não existir. */
    ImportJobDTO getJob(String id);

    Optional<ImportJobDTO> getLatestJob();
}
