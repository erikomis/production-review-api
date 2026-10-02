package com.client.productionreview.metrics;

import com.client.productionreview.model.event.EventType;
import com.client.productionreview.security.ratelimit.RateLimitRule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métricas de negócio expostas em /actuator/prometheus.
 *
 * <ul>
 *   <li>{@code reviewstore_domain_events_total{type,entity}}: ações de negócio concluídas
 *       (avaliações criadas, moderação, importações, logins...).</li>
 *   <li>{@code reviewstore_kafka_publish_total{type,result}}: entrega dos eventos de auditoria ao Kafka
 *       ({@code result} = success | failure).</li>
 *   <li>{@code reviewstore_rate_limit_rejections_total{rule}}: requisições recusadas com 429.</li>
 * </ul>
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        // registra todas as séries com zero: sem isso, o increase() do Prometheus
        // não enxerga a primeira ocorrência de cada tipo de evento
        for (EventType type : EventType.values()) {
            domainEventCounter(type);
            kafkaPublishCounter(type.name(), true);
            kafkaPublishCounter(type.name(), false);
        }
        for (RateLimitRule rule : RateLimitRule.values()) {
            rateLimitCounter(rule.getId());
        }
    }

    public void rateLimitRejected(String rule) {
        rateLimitCounter(rule).increment();
    }

    private Counter rateLimitCounter(String rule) {
        return Counter.builder("reviewstore.rate.limit.rejections")
                .description("Requisições recusadas pelo limite de tentativas, por regra")
                .tag("rule", rule)
                .register(registry);
    }

    public void domainEvent(EventType type) {
        domainEventCounter(type).increment();
    }

    public void kafkaPublish(String type, boolean success) {
        kafkaPublishCounter(type, success).increment();
    }

    private Counter domainEventCounter(EventType type) {
        return Counter.builder("reviewstore.domain.events")
                .description("Ações de negócio concluídas, por tipo de evento")
                .tag("type", type.name())
                .tag("entity", type.getEntityType().name())
                .register(registry);
    }

    private Counter kafkaPublishCounter(String type, boolean success) {
        return Counter.builder("reviewstore.kafka.publish")
                .description("Entregas de eventos de auditoria ao Kafka")
                .tag("type", type == null ? "UNKNOWN" : type)
                .tag("result", success ? "success" : "failure")
                .register(registry);
    }
}
