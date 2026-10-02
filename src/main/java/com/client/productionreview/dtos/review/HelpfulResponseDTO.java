package com.client.productionreview.dtos.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelpfulResponseDTO {

    private Long reviewId;

    private long helpfulCount;

    private boolean helpfulByMe;
}
