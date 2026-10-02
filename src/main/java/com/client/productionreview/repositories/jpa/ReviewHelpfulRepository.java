package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.ReviewHelpful;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, ReviewHelpful.Key> {

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);

    @Query("SELECT h.reviewId FROM ReviewHelpful h WHERE h.userId = :userId AND h.reviewId IN :reviewIds")
    List<Long> findReviewIdsMarkedBy(@Param("userId") Long userId, @Param("reviewIds") Collection<Long> reviewIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReviewHelpful h WHERE h.reviewId = :reviewId AND h.userId = :userId")
    int deleteMark(@Param("reviewId") Long reviewId, @Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReviewHelpful h WHERE h.reviewId = :reviewId")
    int deleteByReview(@Param("reviewId") Long reviewId);
}
