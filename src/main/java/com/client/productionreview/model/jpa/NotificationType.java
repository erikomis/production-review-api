package com.client.productionreview.model.jpa;

public enum NotificationType {
    REVIEW_HIDDEN,
    REVIEW_RESTORED,
    /** Agregada: no máximo uma não lida por review, com a contagem atualizada. */
    REVIEW_HELPFUL,
    REVIEW_REPLIED,
    FOLLOWED_PRODUCT_REVIEW
}
