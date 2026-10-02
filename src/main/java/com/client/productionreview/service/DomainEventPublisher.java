package com.client.productionreview.service;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.User;

/**
 * Publica eventos de domínio no Kafka. Nunca lança exceção: falhas viram log de aviso.
 * Dentro de uma transação, o envio acontece só depois do commit.
 */
public interface DomainEventPublisher {

    /** Usa como autor o usuário autenticado da requisição, se houver. */
    NotificationDto publish(EventType type, Object entityId, String message);

    NotificationDto publish(EventType type, Object entityId, String message, User actor);
}
