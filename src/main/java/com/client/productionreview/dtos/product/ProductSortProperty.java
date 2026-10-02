package com.client.productionreview.dtos.product;

import com.client.productionreview.exception.BadRequestException;

import java.util.Arrays;

/** Propriedades aceitas em {@code property} na listagem de produtos. */
public enum ProductSortProperty {
    name,
    createdAt,
    averageNote,
    totalReviews;

    public static ProductSortProperty from(String value) {
        return Arrays.stream(values())
                .filter(p -> p.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Propriedade de ordenação inválida: " + value));
    }
}
