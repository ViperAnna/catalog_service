package ru.klimovich.order_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.klimovich.order_service.dto.Response;
import ru.klimovich.order_service.util.MessageKeys;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Единый формат ошибок order-service: тело {message, time}.
 * Стандартные исключения Spring MVC (405, 400 при несоответствии типа и нечитаемом теле) обрабатывает
 * базовый класс, их тело заменяется в handleExceptionInternal.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE = "Internal server error";

    private ResponseEntity<Object> createErrorResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new Response(message, LocalDateTime.now()), status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return createErrorResponse("Validation failed: " + fieldErrors, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        Response errorBody = new Response(resolveMessage(ex, body, statusCode), LocalDateTime.now());
        return super.handleExceptionInternal(ex, errorBody, headers, statusCode, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        return createErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceConflictException.class)
    protected ResponseEntity<Object> handleResourceConflict(ResourceConflictException ex) {
        return createErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<Object> handleBadRequest(BadRequestException ex) {
        return createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CatalogServiceException.class)
    protected ResponseEntity<Object> handleCatalogUnavailable(CatalogServiceException ex) {
        return createErrorResponse(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Корзина заблокирована другой транзакцией дольше lock_timeout.
     */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    protected ResponseEntity<Object> handleLockTimeout(PessimisticLockingFailureException ex) {
        return createErrorResponse(MessageKeys.CART_IS_BUSY, HttpStatus.CONFLICT);
    }

    /**
     * Заказ изменен параллельным запросом (optimistic lock по version).
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    protected ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return createErrorResponse("Resource was modified by another request. Try again.", HttpStatus.CONFLICT);
    }

    /**
     * 401 и 403 формирует Spring Security (ExceptionTranslationFilter), поэтому исключение пробрасывается.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    protected void rethrowSecurityException(RuntimeException ex) {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return createErrorResponse(INTERNAL_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static String resolveMessage(Exception ex, @Nullable Object body, HttpStatusCode statusCode) {
        String detail = null;
        if (body instanceof ProblemDetail problemDetail) {
            detail = problemDetail.getDetail();
        } else if (ex instanceof ErrorResponse errorResponse) {
            detail = errorResponse.getBody().getDetail();
        }
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.getReasonPhrase() : INTERNAL_ERROR_MESSAGE;
    }
}
