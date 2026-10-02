package com.client.productionreview.dtos.review;

import com.client.productionreview.model.jpa.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    private Instant createdAt;

    /** Preenchido nas listagens; nulo nas respostas de criação/edição. */
    private String productName;

    private String productSlug;

    private String userName;

    /** Para linkar o perfil público ({@code /u/{username}}). */
    private String userUsername;

    private long helpfulCount;

    /** false quando não há usuário logado. */
    private boolean helpfulByMe;

    private ReviewStatus status;

    private String moderationReason;

    private Instant moderatedAt;

    /** Nome do admin que moderou por último (só faz sentido na moderação). */
    private String moderatedByName;

    @Builder.Default
    private List<ReviewImageDTO> images = new ArrayList<>();

    /** true se o usuário logado já denunciou a review. */
    private boolean reportedByMe;

    /** Denúncias pendentes (preenchido só na listagem de moderação). */
    private long reportsCount;

    /** Resposta oficial da equipe; null se não houver. */
    private ReviewReplyDTO reply;

    /** Usado pela projeção JPQL das listagens. */
    public ReviewResponseDTO(Long id, String title, String description, Long note, Long productId, Long userId,
                             Instant createdAt, String productName, String productSlug, String userName,
                             String userUsername, ReviewStatus status, String moderationReason, Instant moderatedAt,
                             String moderatedByName, String replyText, Instant repliedAt, String replyAuthorName,
                             Long helpfulCount) {
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
        this.userUsername = userUsername == null ? null : userUsername.trim();
        this.reply = replyText == null ? null : new ReviewReplyDTO(replyText, replyAuthorName, repliedAt);
        this.images = new ArrayList<>();
        this.helpfulCount = helpfulCount == null ? 0L : helpfulCount;
    }
}
