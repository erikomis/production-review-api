package com.client.productionreview.service;

import com.client.productionreview.dtos.notification.NotificationResponseDTO;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Notificações do site (+ e-mail assíncrono para ocultação, resposta oficial e produto seguido).
 * Os métodos de disparo nunca lançam exceção: falhar ao notificar não desfaz a operação principal.
 */
public interface NotificationService {

    Page<NotificationResponseDTO> list(Long userId, boolean unreadOnly, Pageable pageable);

    long unreadCount(Long userId);

    /** 404 se a notificação não existir ou for de outro usuário. */
    void markRead(Long notificationId, Long userId);

    void markAllRead(Long userId);

    void reviewHidden(Review review, String reason);

    void reviewRestored(Review review);

    /** Agregada: atualiza a notificação não lida da review em vez de criar outra. */
    void reviewHelpful(Review review, long helpfulCount);

    void reviewReplied(Review review, String replyText);

    /** Avisa os seguidores do produto (exceto o autor) sobre uma nova review visível. */
    void followedProductReview(Review review, Product product, String authorName);
}
