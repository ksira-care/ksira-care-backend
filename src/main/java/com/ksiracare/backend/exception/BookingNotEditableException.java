package com.ksiracare.backend.exception;

/** An admin change to a booking outside its action window (too early, or already started). */
public class BookingNotEditableException extends ApiException {

    public BookingNotEditableException(String detail) {
        super(ErrorCode.BOOKING_NOT_EDITABLE, detail);
    }
}
