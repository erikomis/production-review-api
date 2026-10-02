package com.client.productionreview.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    @Test
    void normalize_removesAccentsLowercasesAndCollapsesSpaces() {
        assertEquals("cafe com leite", TextNormalizer.normalize("  Café   com\tLEITE "));
        assertEquals("pao de queijo acucar", TextNormalizer.normalize("Pão de Queijo Açúcar"));
        assertEquals("", TextNormalizer.normalize("   "));
        assertNull(TextNormalizer.normalize(null));
    }

    @Test
    void escapeLike_escapesWildcards() {
        assertEquals("100!% suco!_uva!!", TextNormalizer.escapeLike("100% suco_uva!"));
    }
}
