package com.client.productionreview.exception;

import com.client.productionreview.dtos.error.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/** Escreve o erro no formato do {@code ResourceHandler} em filtros que rodam fora do MVC. */
public final class ErrorResponses {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ErrorResponses() {
    }

    public static void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getOutputStream(), ErrorResponseDto.builder()
                .message(message)
                .httpStatus(status)
                .statusCode(status.value())
                .build());
    }
}
