package com.client.productionreview.dtos.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDTO {


    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Note is required")
    @Min(value = 1, message = "Note must be between 1 and 5")
    @Max(value = 5, message = "Note must be between 1 and 5")
    private  Long  note;

    @NotNull(message = "ProductId is required")
    private Long productId;
}
