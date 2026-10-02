package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    long countByReviewId(Long reviewId);

    List<ReviewImage> findByReviewIdOrderByIdAsc(Long reviewId);

    List<ReviewImage> findByReviewIdInOrderByIdAsc(Collection<Long> reviewIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReviewImage i WHERE i.reviewId = :reviewId")
    int deleteByReview(@Param("reviewId") Long reviewId);
}
