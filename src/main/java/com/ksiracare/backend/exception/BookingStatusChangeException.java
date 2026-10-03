package com.ksiracare.backend.exception;

/** A booking status change the rules don't allow (e.g. marking before the session starts). */
public class BookingStatusChangeException extends ApiException {

    public BookingStatusChangeException(String detail) {
        super(ErrorCode.BOOKING_STATUS_CHANGE_NOT_ALLOWED, detail);
    }
}
