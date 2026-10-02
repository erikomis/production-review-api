package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    @Query("SELECT COUNT(r) AS totalReviews, AVG(r.note) AS averageNote FROM Review r WHERE r.productId = :productId")
    RatingSummary getRatingSummary(@Param("productId") Long productId);

    interface RatingSummary {
        Long getTotalReviews();

        Double getAverageNote();
    }
}
