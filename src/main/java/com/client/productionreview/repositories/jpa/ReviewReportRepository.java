package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);

    @Query("SELECT r.reviewId FROM ReviewReport r WHERE r.userId = :userId AND r.reviewId IN :reviewIds")
    List<Long> findReviewIdsReportedBy(@Param("userId") Long userId, @Param("reviewIds") Collection<Long> reviewIds);

    @Query("SELECT r.reviewId AS reviewId, COUNT(r) AS total FROM ReviewReport r WHERE r.reviewId IN :reviewIds GROUP BY r.reviewId")
    List<ReviewCount> countByReviews(@Param("reviewIds") Collection<Long> reviewIds);

    /** Denúncias da review com o nome de quem denunciou, mais recentes primeiro. */
    @Query("SELECT r.id AS id, r.reason AS reason, r.details AS details, u.name AS reporterName, r.createdAt AS createdAt "
            + "FROM ReviewReport r LEFT JOIN User u ON u.id = r.userId WHERE r.reviewId = :reviewId "
            + "ORDER BY r.createdAt DESC, r.id DESC")
    List<ReportView> findViewsByReview(@Param("reviewId") Long reviewId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReviewReport r WHERE r.reviewId = :reviewId")
    int deleteByReview(@Param("reviewId") Long reviewId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReviewReport r WHERE r.reviewId IN :reviewIds")
    int deleteByReviews(@Param("reviewIds") Collection<Long> reviewIds);

    interface ReviewCount {
        Long getReviewId();

        Long getTotal();
    }

    interface ReportView {
        Long getId();

        com.client.productionreview.model.jpa.ReportReason getReason();

        String getDetails();

        String getReporterName();

        Instant getCreatedAt();
    }
}
