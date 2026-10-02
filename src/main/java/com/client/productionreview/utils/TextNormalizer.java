package com.client.productionreview.utils;

import java.text.Normalizer;
import java.util.Locale;

/** Normalização usada na busca e na detecção de duplicados: minúsculas, sem acento, espaços colapsados. */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** "  Café   com LEITE " -> "cafe com leite"; null continua null. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Caractere de escape dos LIKEs ({@code ESCAPE '!'}): a barra invertida é especial no MariaDB. */
    public static final char LIKE_ESCAPE = '!';

    /** Escapa os curingas do LIKE ({@code !}, {@code %} e {@code _}) para uso com {@code ESCAPE '!'}. */
    public static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
