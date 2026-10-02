package com.client.productionreview.model.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Review implements Serializable {
    private static final long serialVersionUID = 7L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Column(name = "user_id")
    private Long userId;

//    private String image;

    private  Long  note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id",insertable = false, updatable = false)
    private Product product;

    @Column(name = "product_id")
    private Long productId;


    /** Reviews ocultadas pela moderação somem das listagens públicas e das médias. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @Column(name = "moderation_reason")
    private String moderationReason;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "moderated_by")
    private Long moderatedBy;

    /** Resposta oficial da equipe (uma por review). */
    @Column(name = "reply_text", length = 1000)
    private String replyText;

    @Column(name = "reply_author_id")
    private Long replyAuthorId;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private  Instant updatedAt;

    @PrePersist
    void defaultStatus() {
        if (status == null) {
            status = ReviewStatus.VISIBLE;
        }
    }

}
