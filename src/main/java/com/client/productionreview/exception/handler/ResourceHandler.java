package com.client.productionreview.exception.handler;

import com.client.productionreview.dtos.error.ErrorResponseDto;
import com.client.productionreview.exception.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ResourceHandler {


    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponseDto> handleGlobal(GlobalException e) {
        HttpStatus errorCode = e.getHttpStatus() != null ? e.getHttpStatus() : HttpStatus.BAD_REQUEST;
        return build(errorCode, e.getMensage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFoundException(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequestException(BadRequestException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(BusinessExcepion.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(BusinessExcepion e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(ResponseStatusException e) {
        HttpStatusCode statusCode = e.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return build(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, e.getReason());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleNotReadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido");
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponseDto> handlePropertyReference(PropertyReferenceException e) {
        return build(HttpStatus.BAD_REQUEST, "Propriedade de ordenação inválida: " + e.getPropertyName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrity(DataIntegrityViolationException e) {
        // a mensagem original contém o SQL; não deve ser exposta ao cliente
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Operação viola uma restrição de dados (registro duplicado ou em uso)");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUpload(MaxUploadSizeExceededException e) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o tamanho máximo permitido");
    }

    @ExceptionHandler(IoFileException.class)
    public ResponseEntity<ErrorResponseDto> handleIoFileException(IoFileException e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponseDto.builder()
                .message(message)
                .httpStatus(status)
                .statusCode(status.value())
                .build());
    }

}
