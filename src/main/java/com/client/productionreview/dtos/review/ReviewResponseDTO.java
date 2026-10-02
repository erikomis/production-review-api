package com.client.productionreview.dtos.review;

import com.client.productionreview.model.jpa.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String description;

    private  Long  note;

    private Long productId;

    private Long userId;

    private LocalDateTime createdAt;

    /** Preenchido nas listagens; nulo nas respostas de criação/edição. */
    private String productName;

    private String productSlug;

    private String userName;

    private long helpfulCount;

    /** false quando não há usuário logado. */
    private boolean helpfulByMe;

    private ReviewStatus status;

    private String moderationReason;

    private LocalDateTime moderatedAt;

    /** Nome do admin que moderou por último (só faz sentido na moderação). */
    private String moderatedByName;

    /** Usado pela projeção JPQL das listagens. */
    public ReviewResponseDTO(Long id, String title, String description, Long note, Long productId, Long userId,
                             LocalDateTime createdAt, String productName, String productSlug, String userName,
                             ReviewStatus status, String moderationReason, LocalDateTime moderatedAt,
                             String moderatedByName, Long helpfulCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.note = note;
        this.productId = productId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.productName = productName;
        this.productSlug = productSlug;
        this.userName = userName;
        this.status = status;
        this.moderationReason = moderationReason;
        this.moderatedAt = moderatedAt;
        this.moderatedByName = moderatedByName;
        this.helpfulCount = helpfulCount == null ? 0L : helpfulCount;
    }
}
