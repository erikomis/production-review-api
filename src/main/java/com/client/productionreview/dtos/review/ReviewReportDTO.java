package com.client.productionreview.dtos.review;

import com.client.productionreview.model.jpa.ReportReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportDTO {

    private Long id;

    private ReportReason reason;

    private String details;

    private String reporterName;

    private Instant createdAt;
}
