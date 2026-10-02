package com.client.productionreview.dtos.product;

import java.io.Serializable;

/**
 * Filtros da listagem de produtos. {@code productId} e {@code slug} são usados
 * internamente para montar o detalhe de um produto com a mesma consulta;
 * {@code followedBy} lista os produtos seguidos por um usuário.
 */
public record ProductFilter(String search, Long categoryId, Long subCategorieId, boolean onlyRated,
                            Long productId, String slug, Long followedBy) implements Serializable {

    public ProductFilter(String search, Long categoryId, Long subCategorieId, boolean onlyRated) {
        this(search, categoryId, subCategorieId, onlyRated, null, null, null);
    }

    public static ProductFilter byId(Long id) {
        return new ProductFilter(null, null, null, false, id, null, null);
    }

    public static ProductFilter bySlug(String slug) {
        return new ProductFilter(null, null, null, false, null, slug, null);
    }

    public static ProductFilter followedBy(Long userId) {
        return new ProductFilter(null, null, null, false, null, null, userId);
    }
}
