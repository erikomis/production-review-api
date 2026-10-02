package com.client.productionreview.dtos.review;

import com.client.productionreview.exception.BadRequestException;

import java.util.Arrays;

/** Ordenações aceitas na listagem de reviews de um produto. */
public enum ReviewSort {
    recent,
    oldest,
    highest,
    lowest,
    helpful;

    public static ReviewSort from(String value) {
        if (value == null || value.isBlank()) {
            return recent;
        }
        return Arrays.stream(values())
                .filter(sort -> sort.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Ordenação inválida: " + value));
    }
}
