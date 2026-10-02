package com.client.productionreview.dtos.review;

import com.client.productionreview.model.jpa.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewModerationRequestDTO {

    @NotNull(message = "Status is required")
    private ReviewStatus status;

    /** Obrigatório ao ocultar. */
    @Size(max = 255, message = "Reason must have at most 255 characters")
    private String reason;
}
