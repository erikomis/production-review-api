package com.client.productionreview.service;

import com.client.productionreview.dtos.product.FollowResponseDTO;
import com.client.productionreview.dtos.product.ProductDetailDTO;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductFollowService {

    /** Idempotente; 404 se o produto não existir. */
    FollowResponseDTO follow(Long productId, User user);

    FollowResponseDTO unfollow(Long productId, User user);

    Page<ProductSummaryDTO> following(Long userId, Pageable pageable);

    /** Preenche {@code followersCount} e {@code followedByMe} (usuário logado, se houver). */
    ProductDetailDTO withFollowInfo(ProductDetailDTO detail);
}
