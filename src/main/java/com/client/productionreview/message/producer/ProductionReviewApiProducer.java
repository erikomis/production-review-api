package com.client.productionreview.message.producer;

import com.client.productionreview.dtos.NotificationDto;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductionReviewApiProducer {

    private String topic = "production-review-api";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private  final Gson gson = new Gson();


    public void sendNotification(NotificationDto notificationDto) {
        kafkaTemplate.send(topic, gson.toJson(notificationDto));
    }
}
