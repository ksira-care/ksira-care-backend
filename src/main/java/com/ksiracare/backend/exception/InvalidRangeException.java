package com.ksiracare.backend.exception;

/** A requested time range that is empty, reversed or longer than the API allows. */
public class InvalidRangeException extends ApiException {

    public InvalidRangeException(String detail) {
        super(ErrorCode.VALIDATION_FAILED, detail);
    }
}
