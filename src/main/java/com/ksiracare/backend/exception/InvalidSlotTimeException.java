package com.ksiracare.backend.exception;

/** An hour that isn't a bookable session start, has already started, or is outside the booking window. */
public class InvalidSlotTimeException extends ApiException {

    public InvalidSlotTimeException(String detail) {
        super(ErrorCode.INVALID_SLOT_TIME, detail);
    }
}
