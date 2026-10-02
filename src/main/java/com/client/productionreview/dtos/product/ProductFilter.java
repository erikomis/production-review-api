package com.client.productionreview.dtos.product;

import java.io.Serializable;

/**
 * Filtros da listagem de produtos. {@code productId} e {@code slug} são usados
 * internamente para montar o detalhe de um produto com a mesma consulta.
 */
public record ProductFilter(String search, Long categoryId, Long subCategorieId, boolean onlyRated,
                            Long productId, String slug) implements Serializable {

    public ProductFilter(String search, Long categoryId, Long subCategorieId, boolean onlyRated) {
        this(search, categoryId, subCategorieId, onlyRated, null, null);
    }

    public static ProductFilter byId(Long id) {
        return new ProductFilter(null, null, null, false, id, null);
    }

    public static ProductFilter bySlug(String slug) {
        return new ProductFilter(null, null, null, false, null, slug);
    }
}
