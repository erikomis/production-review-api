package com.client.productionreview.message.producer;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.metrics.BusinessMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionReviewApiProducer {

    private static final String TOPIC = "production-review-api";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final ObjectMapper objectMapper;

    private final BusinessMetrics metrics;


    public void sendNotification(NotificationDto notificationDto) {
        try {
            kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(notificationDto))
                    .whenComplete((result, ex) -> {
                        metrics.kafkaPublish(notificationDto.getType(), ex == null);
                        if (ex != null) {
                            log.warn("Falha ao publicar notificação no Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (JsonProcessingException e) {
            metrics.kafkaPublish(notificationDto.getType(), false);
            log.warn("Falha ao serializar notificação: {}", e.getMessage());
        }
    }
}
