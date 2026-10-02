package com.client.productionreview.repositories.jpa;

import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.model.jpa.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    String DETAILS_SELECT = "SELECT new com.client.productionreview.dtos.review.ReviewResponseDTO("
            + "r.id, r.title, r.description, r.note, r.productId, r.userId, r.createdAt, p.name, u.name) "
            + "FROM Review r LEFT JOIN Product p ON p.id = r.productId LEFT JOIN User u ON u.id = r.userId";

    @Query(value = DETAILS_SELECT, countQuery = "SELECT COUNT(r) FROM Review r")
    Page<ReviewResponseDTO> findAllWithDetails(Pageable pageable);

    @Query(value = DETAILS_SELECT + " WHERE r.productId = :productId",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.productId = :productId")
    Page<ReviewResponseDTO> findByProductIdWithDetails(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT COUNT(r) AS totalReviews, AVG(r.note) AS averageNote FROM Review r WHERE r.productId = :productId")
    RatingSummary getRatingSummary(@Param("productId") Long productId);

    interface RatingSummary {
        Long getTotalReviews();

        Double getAverageNote();
    }
}
