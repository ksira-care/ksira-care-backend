package com.ksiracare.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Turns every failure into an RFC 9457 problem response with a stable {@code code}, e.g.
 * {@code {"status": 409, "code": "SLOT_BOOKED", "title": "Conflict", "detail": "..."}}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException exception) {
        return problem(exception.code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(ErrorCode.VALIDATION_FAILED, detail);
    }

    @ExceptionHandler({
            HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception exception) {
        return problem(ErrorCode.VALIDATION_FAILED, "The request is missing information or has invalid values.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        return problem(ErrorCode.CONCURRENT_UPDATE, "This was changed by someone else at the same time. Please reload and try again.");
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ProblemDetail> handleUnknownEndpoint(Exception exception) {
        return problem(ErrorCode.NOT_FOUND, "No such endpoint.");
    }

    /** Last resort: log the details, but never leak them to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        log.error("Unhandled error", exception);
        return problem(ErrorCode.INTERNAL_ERROR, "Something went wrong on our side.");
    }

    public static ProblemDetail toProblemDetail(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setProperty("code", code.name());
        return problem;
    }

    private static ResponseEntity<ProblemDetail> problem(ErrorCode code, String detail) {
        return ResponseEntity.status(code.status()).body(toProblemDetail(code, detail));
    }
}
