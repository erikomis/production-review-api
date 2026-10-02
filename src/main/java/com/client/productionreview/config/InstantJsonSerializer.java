package com.client.productionreview.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Todas as datas/horas das respostas saem em ISO-8601 UTC com {@code Z}, sem fração de segundo
 * (ex.: {@code "2026-10-02T02:14:49Z"}), igual às lidas do banco.
 */
@JsonComponent
public class InstantJsonSerializer extends JsonSerializer<Instant> {

    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeString(format(value));
    }

    public static String format(Instant value) {
        return value == null ? null : DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.SECONDS));
    }

    @Override
    public Class<Instant> handledType() {
        return Instant.class;
    }
}
