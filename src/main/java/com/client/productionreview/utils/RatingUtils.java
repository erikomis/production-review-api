package com.client.productionreview.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RatingUtils {

    private RatingUtils() {
    }

    /** Arredonda para uma casa decimal; null continua null. */
    public static Double round(Double value) {
        return value == null ? null : Math.round(value * 10.0) / 10.0;
    }

    /** Distribuição com todas as chaves de "1" a "5", na ordem. */
    public static Map<String, Long> emptyDistribution() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int note = 1; note <= 5; note++) {
            distribution.put(String.valueOf(note), 0L);
        }
        return distribution;
    }
}
