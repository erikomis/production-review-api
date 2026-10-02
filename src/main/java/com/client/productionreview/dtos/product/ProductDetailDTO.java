package com.client.productionreview.dtos.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/** ProductSummary + todas as imagens do produto. */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProductDetailDTO extends ProductSummaryDTO {

    private static final long serialVersionUID = 1L;

    private List<ProductImageSummaryDTO> images = new ArrayList<>();

    public static ProductDetailDTO from(ProductSummaryDTO summary, List<ProductImageSummaryDTO> images) {
        ProductDetailDTO detail = new ProductDetailDTO();
        detail.setId(summary.getId());
        detail.setName(summary.getName());
        detail.setDescription(summary.getDescription());
        detail.setSlug(summary.getSlug());
        detail.setSubCategorieId(summary.getSubCategorieId());
        detail.setSubCategorieName(summary.getSubCategorieName());
        detail.setCategoryId(summary.getCategoryId());
        detail.setCategoryName(summary.getCategoryName());
        detail.setAverageNote(summary.getAverageNote());
        detail.setTotalReviews(summary.getTotalReviews());
        detail.setCreatedAt(summary.getCreatedAt());
        detail.setImages(new ArrayList<>(images));
        detail.setImageUrl(images.isEmpty() ? null : images.get(0).getUrlImage());
        return detail;
    }
}
