package com.client.productionreview.dtos.product;

import com.client.productionreview.utils.RatingUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Produto da listagem pública, com nota média e total de reviews visíveis. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String slug;
    private Long subCategorieId;
    private String subCategorieName;
    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private Double averageNote;
    private long totalReviews;
    private LocalDateTime createdAt;

    /** Usado pela projeção JPQL. */
    public ProductSummaryDTO(Long id, String name, String description, String slug,
                             Long subCategorieId, String subCategorieName, Long categoryId, String categoryName,
                             LocalDateTime createdAt, Double averageNote, Long totalReviews) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.slug = slug;
        this.subCategorieId = subCategorieId;
        this.subCategorieName = subCategorieName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.createdAt = createdAt;
        this.totalReviews = totalReviews == null ? 0L : totalReviews;
        this.averageNote = this.totalReviews == 0 ? null : RatingUtils.round(averageNote);
    }
}
