package com.client.productionreview.integration;

import com.client.productionreview.dtos.importer.OpenFoodFactsProduct;

import java.util.List;

/** Cliente da API pública do Open Food Facts (dados abertos, ODbL). */
public interface OpenFoodFactsClient {

    /**
     * Busca produtos vendidos no Brasil de uma categoria (tag em inglês), ordenados por popularidade.
     * Respeita o intervalo mínimo entre buscas e faz um retry em 429/503/resposta não-JSON.
     *
     * @throws OpenFoodFactsException quando a busca falha
     */
    List<OpenFoodFactsProduct> searchProducts(String categoryTag, int pageSize);

    class OpenFoodFactsException extends RuntimeException {
        public OpenFoodFactsException(String message) {
            super(message);
        }
    }
}
