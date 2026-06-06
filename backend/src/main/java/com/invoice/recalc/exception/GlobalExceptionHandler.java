package com.invoice.recalc.exception;

import com.invoice.recalc.dto.InvoiceDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<InvoiceDto.ErrorResponse> handleNotFound(InvoiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildError(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage()));
    }

    @ExceptionHandler(RecalculationLimitExceededException.class)
    public ResponseEntity<InvoiceDto.ErrorResponse> handleLimitExceeded(RecalculationLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Límite de recálculo excedido", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<InvoiceDto.ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildError(HttpStatus.BAD_REQUEST, "Argumento inválido", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<InvoiceDto.ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildError(HttpStatus.BAD_REQUEST, "Error de validación", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<InvoiceDto.ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", ex.getMessage()));
    }

    private InvoiceDto.ErrorResponse buildError(HttpStatus status, String error, String message) {
        InvoiceDto.ErrorResponse r = new InvoiceDto.ErrorResponse();
        r.setStatus(status.value());
        r.setError(error);
        r.setMessage(message);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }
}
