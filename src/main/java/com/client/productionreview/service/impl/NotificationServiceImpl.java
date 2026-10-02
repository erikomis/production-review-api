package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.notification.NotificationResponseDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.integration.NotificationMailer;
import com.client.productionreview.model.jpa.Notification;
import com.client.productionreview.model.jpa.NotificationType;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.NotificationRepository;
import com.client.productionreview.repositories.jpa.ProductFollowRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.NotificationService;
import com.client.productionreview.utils.SlugUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    static final String MY_REVIEWS_PATH = "/minhas-avaliacoes";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductFollowRepository productFollowRepository;
    private final NotificationMailer mailer;
    private final String siteUrl;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository,
                                   ProductRepository productRepository, ProductFollowRepository productFollowRepository,
                                   NotificationMailer mailer,
                                   @Value("${app.frontend-site-url:http://localhost:5174}") String siteUrl) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productFollowRepository = productFollowRepository;
        this.mailer = mailer;
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    @Override
    public Page<NotificationResponseDTO> list(Long userId, boolean unreadOnly, Pageable pageable) {
        return notificationRepository.findForUser(userId, unreadOnly, pageable).map(NotificationResponseDTO::from);
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    @Override
    public void reviewHidden(Review review, String reason) {
        safely("REVIEW_HIDDEN", () -> {
            String title = "Sua avaliação foi ocultada";
            String message = "Sua avaliação \"" + titleOf(review) + "\" foi ocultada pela moderação. Motivo: " + reason;
            notify(review.getUserId(), NotificationType.REVIEW_HIDDEN, title, message, MY_REVIEWS_PATH, review.getId(), true);
        });
    }

    @Override
    public void reviewRestored(Review review) {
        safely("REVIEW_RESTORED", () -> {
            String message = "Sua avaliação \"" + titleOf(review) + "\" voltou a ficar visível no site.";
            notify(review.getUserId(), NotificationType.REVIEW_RESTORED, "Sua avaliação foi restaurada", message,
                    reviewLink(review), review.getId(), false);
        });
    }

    @Override
    public void reviewHelpful(Review review, long helpfulCount) {
        safely("REVIEW_HELPFUL", () -> {
            String message = helpfulCount == 1
                    ? "1 pessoa achou útil a sua avaliação \"" + titleOf(review) + "\"."
                    : helpfulCount + " pessoas acharam útil a sua avaliação \"" + titleOf(review) + "\".";
            Optional<Notification> unread = notificationRepository
                    .findFirstByUserIdAndTypeAndReviewIdAndReadFalseOrderByIdDesc(review.getUserId(),
                            NotificationType.REVIEW_HELPFUL, review.getId());
            if (unread.isPresent()) {
                // no máximo uma não lida por review: atualiza a contagem e sobe para o topo
                Notification notification = unread.get();
                notification.setMessage(message);
                notification.setCreatedAt(Instant.now());
                notificationRepository.save(notification);
                return;
            }
            notify(review.getUserId(), NotificationType.REVIEW_HELPFUL, "Sua avaliação foi útil", message,
                    reviewLink(review), review.getId(), false);
        });
    }

    @Override
    public void reviewReplied(Review review, String replyText) {
        safely("REVIEW_REPLIED", () -> {
            String message = "A equipe ReviewStore respondeu à sua avaliação \"" + titleOf(review) + "\": "
                    + SlugUtils.truncate(replyText, 300);
            notify(review.getUserId(), NotificationType.REVIEW_REPLIED, "Sua avaliação recebeu uma resposta", message,
                    reviewLink(review), review.getId(), true);
        });
    }

    @Override
    public void followedProductReview(Review review, Product product, String authorName) {
        safely("FOLLOWED_PRODUCT_REVIEW", () -> {
            List<Long> followers = productFollowRepository.findFollowerIds(product.getId());
            String productName = product.getName() == null ? "" : product.getName().trim();
            String message = (authorName == null ? "Alguém" : authorName.trim()) + " avaliou " + productName
                    + " com " + review.getNote() + (Objects.equals(review.getNote(), 1L) ? " estrela" : " estrelas")
                    + ": \"" + titleOf(review) + "\"";
            String link = "/products/" + product.getSlug().trim() + "#review-" + review.getId();
            for (Long followerId : followers) {
                if (!Objects.equals(followerId, review.getUserId())) {
                    notify(followerId, NotificationType.FOLLOWED_PRODUCT_REVIEW, "Nova avaliação em " + productName,
                            message, link, review.getId(), true);
                }
            }
        });
    }

    private void notify(Long userId, NotificationType type, String title, String message, String link, Long reviewId,
                        boolean email) {
        if (userId == null) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(SlugUtils.truncate(title, 255))
                .message(SlugUtils.truncate(message, 1000))
                .link(link)
                .reviewId(reviewId)
                .createdAt(Instant.now())
                .build());

        if (email) {
            userRepository.findById(userId)
                    .filter(user -> Boolean.TRUE.equals(user.getActive()) && user.wantsEmailNotifications())
                    .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                    .ifPresent(user -> sendEmail(user, title, message, link));
        }
    }

    private void sendEmail(User user, String title, String message, String link) {
        String to = user.getEmail().trim();
        String body = "Olá " + (user.getName() == null ? "" : user.getName().trim()) + ",\n\n"
                + message + "\n\n"
                + "Veja no site: " + siteUrl + (link == null ? "" : link) + "\n\n"
                + "Para deixar de receber estes e-mails, desative as notificações por e-mail em "
                + siteUrl + "/preferencias.\n\n"
                + "Atenciosamente,\nEquipe ReviewStore";
        String subject = title + " - ReviewStore";
        // dentro de uma transação, só envia depois do commit
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mailer.send(to, subject, body);
                }
            });
        } else {
            mailer.send(to, subject, body);
        }
    }

    private String reviewLink(Review review) {
        return productRepository.findById(review.getProductId())
                .map(product -> "/products/" + product.getSlug().trim() + "#review-" + review.getId())
                .orElse(MY_REVIEWS_PATH);
    }

    private static String titleOf(Review review) {
        return review.getTitle() == null ? "" : review.getTitle().trim();
    }

    private static void safely(String type, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Falha ao registrar notificação {}: {}", type, e.getMessage());
        }
    }
}
