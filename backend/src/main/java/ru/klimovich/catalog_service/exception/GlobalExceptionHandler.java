package ru.klimovich.catalog_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.klimovich.catalog_service.dto.Response;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Object> createErrorResponse(String message, HttpStatus status) {
        Response body = new Response(
                message,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleValidationResourceException(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        String errorMessage = "Validation failed: " + fieldErrors;

        return createErrorResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected Response handleResourceNotFound(ResourceNotFoundException ex) {
        return new Response(
                ex.getMessage(),
                LocalDateTime.now()
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
