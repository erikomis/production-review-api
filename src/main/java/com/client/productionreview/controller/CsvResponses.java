package com.client.productionreview.controller;

import com.client.productionreview.service.impl.StatsServiceImpl;
import com.client.productionreview.utils.CsvWriter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

/** Resposta de download CSV: {@code attachment; filename="avaliacoes-2026-10-02.csv"}. */
final class CsvResponses {

    private CsvResponses() {
    }

    static ResponseEntity<byte[]> attachment(String baseName, byte[] csv) {
        String filename = baseName + "-" + LocalDate.now(StatsServiceImpl.STATS_ZONE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(CsvWriter.CONTENT_TYPE))
                .contentLength(csv.length)
                .body(csv);
    }
}
