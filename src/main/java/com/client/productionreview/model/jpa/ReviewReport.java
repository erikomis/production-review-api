package com.client.productionreview.model.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

/** Denúncia de um usuário sobre uma review (no máximo uma por par review/usuário). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "review_report", uniqueConstraints = @UniqueConstraint(name = "uk_review_report_user",
        columnNames = {"review_id", "user_id"}))
public class ReviewReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    @Column(name = "details", length = 500)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
