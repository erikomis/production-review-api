package com.client.productionreview.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvWriterTest {

    @Test
    void write_hasBomSemicolonAndCrlf() {
        byte[] csv = CsvWriter.write(List.of("ID", "Nome"), List.of(Arrays.asList(1L, "Café"), Arrays.asList(2L, null)));

        assertEquals((byte) 0xEF, csv[0]);
        assertEquals((byte) 0xBB, csv[1]);
        assertEquals((byte) 0xBF, csv[2]);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertEquals("ID;Nome\r\n1;Café\r\n2;\r\n", text);
    }

    @Test
    void cell_quotesSeparatorsQuotesAndLineBreaks() {
        assertEquals("\"a;b\"", CsvWriter.cell("a;b"));
        assertEquals("\"diz \"\"oi\"\"\"", CsvWriter.cell("diz \"oi\""));
        assertEquals("\"linha1\nlinha2\"", CsvWriter.cell("linha1\nlinha2"));
    }

    @Test
    void cell_neutralizesFormulas() {
        assertEquals("'=1+1", CsvWriter.cell("=1+1"));
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", CsvWriter.cell("=HYPERLINK(\"x\")"));
        assertEquals("'+1", CsvWriter.cell("+1"));
        assertEquals("'@SUM(A1)", CsvWriter.cell("@SUM(A1)"));
        assertEquals("-1", CsvWriter.cell(-1L));
    }

    @Test
    void cell_formatsInstantsAsUtcIso() {
        assertEquals("2026-10-02T02:14:49Z", CsvWriter.cell(Instant.parse("2026-10-02T02:14:49.123Z")));
    }
}
