package com.client.productionreview.repositories.jpa;

import com.client.productionreview.dtos.admin.TopCategoryDTO;
import com.client.productionreview.dtos.admin.TopProductDTO;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewQueryRepository {

    /** Só reviews visíveis entram na média e no total. */
    @Query("SELECT COUNT(r) AS totalReviews, AVG(r.note) AS averageNote FROM Review r "
            + "WHERE r.productId = :productId AND r.status = com.client.productionreview.model.jpa.ReviewStatus.VISIBLE")
    RatingSummary getRatingSummary(@Param("productId") Long productId);

    @Query("SELECT r.note AS note, COUNT(r) AS total FROM Review r "
            + "WHERE r.productId = :productId AND r.status = com.client.productionreview.model.jpa.ReviewStatus.VISIBLE "
            + "GROUP BY r.note")
    List<NoteCount> countByNoteForProduct(@Param("productId") Long productId);

    @Query("SELECT r.note AS note, COUNT(r) AS total FROM Review r WHERE r.status = :status GROUP BY r.note")
    List<NoteCount> countByNote(@Param("status") ReviewStatus status);

    long countByStatus(ReviewStatus status);

    @Query("SELECT AVG(r.note) FROM Review r WHERE r.status = :status")
    Double averageNote(@Param("status") ReviewStatus status);

    @Query("SELECT r.createdAt AS createdAt, r.note AS note FROM Review r "
            + "WHERE r.status = :status AND r.createdAt >= :from")
    List<CreatedNote> findCreatedSince(@Param("status") ReviewStatus status, @Param("from") LocalDateTime from);

    @Query("SELECT new com.client.productionreview.dtos.admin.TopProductDTO(p.id, p.name, p.slug, COUNT(r), AVG(r.note)) "
            + "FROM Review r JOIN Product p ON p.id = r.productId WHERE r.status = :status "
            + "GROUP BY p.id, p.name, p.slug ORDER BY COUNT(r) DESC, AVG(r.note) DESC, p.id ASC")
    List<TopProductDTO> topProducts(@Param("status") ReviewStatus status, Pageable pageable);

    @Query("SELECT new com.client.productionreview.dtos.admin.TopCategoryDTO(c.id, c.name, COUNT(r), AVG(r.note)) "
            + "FROM Review r JOIN Product p ON p.id = r.productId JOIN SubCategory s ON s.id = p.subCategorieId "
            + "JOIN Category c ON c.id = s.categorieId WHERE r.status = :status "
            + "GROUP BY c.id, c.name ORDER BY COUNT(r) DESC, AVG(r.note) DESC, c.id ASC")
    List<TopCategoryDTO> topCategories(@Param("status") ReviewStatus status, Pageable pageable);

    @Query("SELECT r.userId AS userId, COUNT(r) AS total FROM Review r WHERE r.userId IN :userIds GROUP BY r.userId")
    List<UserCount> countByUsers(@Param("userIds") Collection<Long> userIds);

    interface RatingSummary {
        Long getTotalReviews();

        Double getAverageNote();
    }

    interface NoteCount {
        Long getNote();

        Long getTotal();
    }

    interface CreatedNote {
        LocalDateTime getCreatedAt();

        Long getNote();
    }

    interface UserCount {
        Long getUserId();

        Long getTotal();
    }
}
