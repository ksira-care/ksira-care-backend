package com.ksiracare.backend.exception;

/** No aapta who speaks the client's languages is free at that hour any more. */
public class TimeNotAvailableException extends ApiException {

    public TimeNotAvailableException() {
        super(ErrorCode.TIME_NOT_AVAILABLE, "That time was just taken. Please pick another.");
    }
}
