package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.message.producer.ProductionReviewApiProducer;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.security.CurrentUser;
import com.client.productionreview.service.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class DomainEventPublisherImpl implements DomainEventPublisher {

    static final String SYSTEM_USER = "Sistema";

    private final ProductionReviewApiProducer producer;

    public DomainEventPublisherImpl(ProductionReviewApiProducer producer) {
        this.producer = producer;
    }

    @Override
    public NotificationDto publish(EventType type, Object entityId, String message) {
        return publish(type, entityId, message, CurrentUser.get().orElse(null));
    }

    @Override
    public NotificationDto publish(EventType type, Object entityId, String message, User actor) {
        try {
            NotificationDto event = NotificationDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .type(type.name())
                    .action(type.getAction())
                    .message(message)
                    .nameUser(actor != null && actor.getName() != null ? actor.getName().trim() : SYSTEM_USER)
                    .userId(actor != null ? actor.getId() : null)
                    .entityType(type.getEntityType().name())
                    .entityId(entityId != null ? String.valueOf(entityId) : null)
                    .occurredAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                    .build();

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                // só publica se a transação for confirmada
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        send(event);
                    }
                });
            } else {
                send(event);
            }
            return event;
        } catch (Exception e) {
            log.warn("Falha ao montar evento {}: {}", type, e.getMessage());
            return null;
        }
    }

    private void send(NotificationDto event) {
        try {
            producer.sendNotification(event);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento {} no Kafka: {}", event.getType(), e.getMessage());
        }
    }
}
