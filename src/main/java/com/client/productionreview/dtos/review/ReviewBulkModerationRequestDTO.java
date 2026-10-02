package com.client.productionreview.dtos.review;

import com.client.productionreview.model.jpa.ReviewStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewBulkModerationRequestDTO {

    @NotEmpty(message = "Informe de 1 a 100 avaliações")
    @Size(max = 100, message = "Informe de 1 a 100 avaliações")
    private List<@NotNull(message = "Informe de 1 a 100 avaliações") Long> ids;

    @NotNull(message = "Status is required")
    private ReviewStatus status;

    /** Obrigatório ao ocultar. */
    @Size(max = 255, message = "Reason must have at most 255 characters")
    private String reason;
}
