package com.client.productionreview.service;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.message.producer.ProductionReviewApiProducer;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.impl.DomainEventPublisherImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private ProductionReviewApiProducer producer;

    @InjectMocks
    private DomainEventPublisherImpl publisher;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publish_fillsAllFieldsWithUserFromSecurityContext() {
        User user = User.builder().id(2L).name("Usuário Teste").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        publisher.publish(EventType.REVIEW_CREATED, 12L, "Usuário Teste avaliou Smartphone X com 5 estrelas");

        ArgumentCaptor<NotificationDto> captor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(producer).sendNotification(captor.capture());
        NotificationDto event = captor.getValue();
        assertNotNull(event.getEventId());
        assertEquals("REVIEW_CREATED", event.getType());
        assertEquals("Avaliação criada", event.getAction());
        assertEquals("Usuário Teste avaliou Smartphone X com 5 estrelas", event.getMessage());
        assertEquals("Usuário Teste", event.getNameUser());
        assertEquals(2L, event.getUserId());
        assertEquals("REVIEW", event.getEntityType());
        assertEquals("12", event.getEntityId());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void publish_withExplicitActor_andWithoutUser() {
        User actor = User.builder().id(5L).name("Novo").build();

        NotificationDto signedUp = publisher.publish(EventType.USER_SIGNED_UP, 5L, "Novo se cadastrou", actor);
        NotificationDto anonymous = publisher.publish(EventType.CATALOG_IMPORT_FAILED, "job", "falhou");

        assertEquals("USER", signedUp.getEntityType());
        assertEquals(5L, signedUp.getUserId());
        assertEquals("Sistema", anonymous.getNameUser());
        assertNull(anonymous.getUserId());
        assertNotEquals(signedUp.getEventId(), anonymous.getEventId());
        verify(producer, times(2)).sendNotification(any());
    }

    @Test
    void publish_neverThrowsWhenKafkaFails() {
        doThrow(new RuntimeException("broker down")).when(producer).sendNotification(any());

        assertDoesNotThrow(() -> publisher.publish(EventType.PRODUCT_CREATED, 1L, "Produto criado"));
    }

    @Test
    void publish_insideTransaction_waitsForCommit() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publish(EventType.CATEGORY_CREATED, 1L, "Categoria criada");
        verifyNoInteractions(producer);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(producer).sendNotification(any());
    }
}
