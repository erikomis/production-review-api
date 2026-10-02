package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.ProductFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface ProductFollowRepository extends JpaRepository<ProductFollow, ProductFollow.Key> {

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    long countByProductId(Long productId);

    @Query("SELECT f.userId FROM ProductFollow f WHERE f.productId = :productId")
    List<Long> findFollowerIds(@Param("productId") Long productId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ProductFollow f WHERE f.productId = :productId AND f.userId = :userId")
    int deleteFollow(@Param("productId") Long productId, @Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ProductFollow f WHERE f.productId IN :productIds")
    int deleteByProducts(@Param("productIds") Collection<Long> productIds);
}
