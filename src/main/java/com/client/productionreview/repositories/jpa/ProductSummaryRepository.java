package com.client.productionreview.repositories.jpa;

import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Consultas de produto com agregados de reviews visíveis (fragmento do ProductRepository). */
public interface ProductSummaryRepository {

    /**
     * Ordenação aceita (via {@code pageable.getSort()}): name, createdAt, averageNote, totalReviews.
     * Sem ordenação, usa o id. averageNote desempata por totalReviews DESC; produtos sem nota vão para o fim.
     */
    Page<ProductSummaryDTO> findSummaries(ProductFilter filter, Pageable pageable);
}
