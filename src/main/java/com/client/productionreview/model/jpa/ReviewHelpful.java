package com.client.productionreview.model.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

/** Marcação "útil" de um usuário em uma review (no máximo uma por par review/usuário). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_helpful")
@IdClass(ReviewHelpful.Key.class)
public class ReviewHelpful implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "review_id")
    private Long reviewId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    public ReviewHelpful(Long reviewId, Long userId) {
        this.reviewId = reviewId;
        this.userId = userId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long reviewId;
        private Long userId;
    }
}
