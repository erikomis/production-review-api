package com.client.productionreview.dtos.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Estado de um job de importação de catálogo (cópia imutável para a resposta). */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobDTO {

    public enum Status { RUNNING, COMPLETED, FAILED }

    private String id;
    private String source;
    private Status status;
    private int totalSteps;
    private int completedSteps;
    private String currentStep;
    private int categoriesCreated;
    private int subCategoriesCreated;
    private int productsCreated;
    private int productsSkipped;
    private int imagesCreated;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    private Instant startedAt;
    private Instant finishedAt;
    private String startedBy;
}
