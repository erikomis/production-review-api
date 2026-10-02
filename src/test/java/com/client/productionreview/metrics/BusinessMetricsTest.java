package com.client.productionreview.metrics;

import com.client.productionreview.model.event.EventType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BusinessMetricsTest {

    @Test
    void registersEverySeriesAtZeroOnStartup() {
        var registry = new SimpleMeterRegistry();
        new BusinessMetrics(registry);

        // o Prometheus só calcula increase() a partir de uma série que já existia com 0
        for (EventType type : EventType.values()) {
            var events = registry.find("reviewstore.domain.events").tag("type", type.name()).counter();
            assertNotNull(events, type.name());
            assertEquals(0.0, events.count());
            assertNotNull(registry.find("reviewstore.kafka.publish").tag("type", type.name()).tag("result", "success").counter());
            assertNotNull(registry.find("reviewstore.kafka.publish").tag("type", type.name()).tag("result", "failure").counter());
        }
    }

    @Test
    void incrementsTheSameSeriesThatWasPreRegistered() {
        var registry = new SimpleMeterRegistry();
        var metrics = new BusinessMetrics(registry);

        metrics.domainEvent(EventType.USER_LOGGED_IN);
        metrics.kafkaPublish("USER_LOGGED_IN", false);

        assertEquals(1.0, registry.find("reviewstore.domain.events").tag("type", "USER_LOGGED_IN").counter().count());
        assertEquals(1.0, registry.find("reviewstore.kafka.publish").tag("type", "USER_LOGGED_IN").tag("result", "failure").counter().count());
        assertEquals(EventType.values().length, registry.find("reviewstore.domain.events").counters().size());
    }
}
