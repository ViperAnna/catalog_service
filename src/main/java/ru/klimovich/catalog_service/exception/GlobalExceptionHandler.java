package ru.klimovich.catalog_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.klimovich.catalog_service.model.Response;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<Object> createErrorResponse(String message, HttpStatus status) {
        Response body = new Response(
                message,
                status.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(ValidationResourceException.class)
    protected ResponseEntity<Object> handleValidationResourceException(ValidationResourceException ex) {
        return createErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        return createErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(ResourceConflictException.class)
    protected ResponseEntity<Object> handleResourceConflict(ResourceConflictException ex) {
        return createErrorResponse(
                ex.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}
