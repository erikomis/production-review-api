package com.client.productionreview.message;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.message.producer.ProductionReviewApiProducer;
import com.client.productionreview.metrics.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductionReviewApiProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    private SimpleMeterRegistry registry;

    private ProductionReviewApiProducer producer;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        producer = new ProductionReviewApiProducer(kafkaTemplate,
                new ObjectMapper().registerModule(new JavaTimeModule()), new BusinessMetrics(registry));
    }

    private double published(String result) {
        var counter = registry.find("reviewstore.kafka.publish").tag("type", "REVIEW_CREATED").tag("result", result).counter();
        return counter == null ? 0 : counter.count();
    }

    private NotificationDto event() {
        return NotificationDto.builder().eventId("e1").type("REVIEW_CREATED").action("Avaliação criada").build();
    }

    @Test
    void successfulDelivery_isCountedAsSuccess() {
        when(kafkaTemplate.send(eq("production-review-api"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        producer.sendNotification(event());

        assertEquals(1.0, published("success"));
        assertEquals(0.0, published("failure"));
    }

    @Test
    void failedDelivery_isCountedAsFailureWithoutThrowing() {
        when(kafkaTemplate.send(eq("production-review-api"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker fora do ar")));

        producer.sendNotification(event());

        assertEquals(1.0, published("failure"));
    }
}
