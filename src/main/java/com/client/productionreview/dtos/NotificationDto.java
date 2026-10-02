package com.client.productionreview.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Evento de domínio publicado no tópico do Kafka e emitido no SSE.
 * {@code action}, {@code message} e {@code nameUser} são mantidos por compatibilidade
 * com o consumidor antigo; os demais campos foram acrescentados na auditoria.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {

    private String eventId;
    private String type;
    private String action;
    private String message;
    private String nameUser;
    private Long userId;
    private String entityType;
    private String entityId;
    private Instant occurredAt;

    public NotificationDto(String action, String message, String nameUser) {
        this.action = action;
        this.message = message;
        this.nameUser = nameUser;
    }
}
