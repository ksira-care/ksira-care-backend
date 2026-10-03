package com.ksiracare.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes sent in every error response ({@code code}).
 * Clients branch on these, never on the human-readable {@code detail}.
 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    SLOT_BOOKED(HttpStatus.CONFLICT),
    INVALID_SLOT_TIME(HttpStatus.UNPROCESSABLE_CONTENT),
    BOOKING_STATUS_CHANGE_NOT_ALLOWED(HttpStatus.CONFLICT),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
