package com.client.productionreview.dtos.review;

import com.client.productionreview.model.jpa.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportRequestDTO {

    @NotNull(message = "O motivo é obrigatório")
    private ReportReason reason;

    @Size(max = 500, message = "Os detalhes devem ter no máximo 500 caracteres")
    private String details;
}
