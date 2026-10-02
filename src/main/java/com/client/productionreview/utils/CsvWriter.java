package com.client.productionreview.utils;

import com.client.productionreview.config.InstantJsonSerializer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * CSV no formato que o Excel pt-BR abre direto: UTF-8 com BOM, separador {@code ;} e linhas CRLF.
 * Células que começam com {@code = + - @} recebem um apóstrofo (evita injeção de fórmulas).
 */
public final class CsvWriter {

    public static final String CONTENT_TYPE = "text/csv;charset=UTF-8";

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final char SEPARATOR = ';';

    private CsvWriter() {
    }

    public static byte[] write(List<String> header, List<List<?>> rows) {
        StringBuilder csv = new StringBuilder();
        appendLine(csv, header);
        rows.forEach(row -> appendLine(csv, row));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(BOM);
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static void appendLine(StringBuilder csv, List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(SEPARATOR);
            }
            csv.append(cell(cells.get(i)));
        }
        csv.append("\r\n");
    }

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value instanceof Instant instant ? InstantJsonSerializer.format(instant) : String.valueOf(value).trim();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0 && !(value instanceof Number)) {
            text = "'" + text;
        }
        if (text.indexOf(SEPARATOR) >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
