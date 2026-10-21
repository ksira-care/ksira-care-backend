package com.ksiracare.backend.exception;

/** The chosen aapta hasn't opened that hour (or it was taken meanwhile). */
public class AaptaNotAvailableException extends ApiException {

    public AaptaNotAvailableException(String detail) {
        super(ErrorCode.AAPTA_NOT_AVAILABLE, detail);
    }
}
