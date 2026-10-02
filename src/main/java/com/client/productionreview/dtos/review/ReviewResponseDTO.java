package com.client.productionreview.dtos.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO {

    private Long id;

    private String title;

    private String description;

    private  Long  note;

    private Long productId;

    private Long userId;

    private LocalDateTime createdAt;

    /** Preenchido nas listagens; nulo nas respostas de criação/edição. */
    private String productName;

    private String userName;
}
