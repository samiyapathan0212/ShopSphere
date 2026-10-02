package com.shopsphere.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shopsphere.backend.dto.response.ApiErrorResponse;
import com.shopsphere.backend.security.RestAccessDeniedHandler;

/**
 * Centralized exception handling for validation, conflict, authentication
 * (401) and authorization (403) errors. No request payloads ever include
 * passwords, so nothing sensitive can be surfaced here.
 * <p>
 * Phase 5 (checkout, orders, payments, inventory) registers the same envelope
 * here so those flows surface their documented 400/404/409 statuses instead of
 * falling through to the generic 500 handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Validation failed",
                        fieldErrors));
    }

    @ExceptionHandler(InvalidQueryParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidQueryParameter(InvalidQueryParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Malformed request body"));
    }

    @ExceptionHandler({EmptyCartException.class, InvalidQuantityException.class})
    public ResponseEntity<ApiErrorResponse> handlePurchaseBadRequest(RuntimeException ex) {
        // 400: the request itself cannot be carried out as described — there is
        // nothing to buy, or a quantity guard rejected the value.
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailConflict(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        ex.getMessage()));
    }

    @ExceptionHandler({CategoryNotFoundException.class, ProductNotFoundException.class,
            WishlistNotFoundException.class, WishlistItemNotFoundException.class,
            // Phase 5: a foreign order/payment is deliberately indistinguishable
            // from a missing one, so all of these are plain 404s.
            OrderNotFoundException.class, PaymentNotFoundException.class,
            CartNotFoundException.class, CartItemNotFoundException.class,
            InventoryNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage()));
    }

    @ExceptionHandler({DuplicateCategoryNameException.class, DuplicateSkuException.class,
            CategoryInUseException.class, WishlistItemAlreadyExistsException.class})
    public ResponseEntity<ApiErrorResponse> handleCatalogConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        ex.getMessage()));
    }

    @ExceptionHandler({InsufficientStockException.class, OrderCancellationException.class,
            PaymentStateException.class, InvalidOrderStatusTransitionException.class})
    public ResponseEntity<ApiErrorResponse> handleOrderConflict(RuntimeException ex) {
        // 409: the request is well formed but conflicts with current state —
        // stock cannot cover the demand, the order can no longer be cancelled,
        // the payment is in an unsupported state, or the status transition is
        // rejected by the order lifecycle state machine.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex) {
        // Generic message: does not reveal whether the email exists or whether
        // the password was wrong.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        "Invalid email or password"));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        // Generic 401: never reveals whether a token/user exists.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        // Clean JSON 403 for authenticated users lacking the required role.
        // Same generic message as the filter-chain handler; never reveals which
        // rule failed or any detail about the caller.
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        HttpStatus.FORBIDDEN.getReasonPhrase(),
                        RestAccessDeniedHandler.FORBIDDEN_MESSAGE));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Backstop when a unique constraint is hit outside the pre-check (e.g. a race).
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        "Request conflicts with existing data"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "An unexpected error occurred"));
    }
}