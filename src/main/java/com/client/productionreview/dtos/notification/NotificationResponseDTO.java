package com.client.productionreview.dtos.notification;

import com.client.productionreview.model.jpa.Notification;
import com.client.productionreview.model.jpa.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    /** Caminho do site (ex.: {@code /products/cafe#review-12}). */
    private String link;
    private boolean read;
    private Instant createdAt;

    public static NotificationResponseDTO from(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
