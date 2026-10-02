package com.client.productionreview.dtos.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponseDTO {

    private boolean following;

    private long followersCount;
}
