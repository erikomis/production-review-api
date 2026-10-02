package com.client.productionreview.dtos.admin;

import com.client.productionreview.utils.RatingUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TopCategoryDTO {

    private Long id;
    private String name;
    private long totalReviews;
    private Double averageNote;

    public TopCategoryDTO(Long id, String name, Long totalReviews, Double averageNote) {
        this.id = id;
        this.name = name;
        this.totalReviews = totalReviews == null ? 0L : totalReviews;
        this.averageNote = RatingUtils.round(averageNote);
    }
}
