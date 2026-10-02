package com.client.productionreview.dtos.admin;

import com.client.productionreview.utils.RatingUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TopProductDTO {

    private Long id;
    private String name;
    private String slug;
    private long totalReviews;
    private Double averageNote;

    public TopProductDTO(Long id, String name, String slug, Long totalReviews, Double averageNote) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.totalReviews = totalReviews == null ? 0L : totalReviews;
        this.averageNote = RatingUtils.round(averageNote);
    }
}
