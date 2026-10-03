package com.ksiracare.backend.exception;

/** Too many failed sign-ins in a short time. */
public class TooManyLoginAttemptsException extends ApiException {

    public TooManyLoginAttemptsException(String detail) {
        super(ErrorCode.RATE_LIMITED, detail);
    }
}
