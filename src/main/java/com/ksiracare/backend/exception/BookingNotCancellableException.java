package com.ksiracare.backend.exception;

/** A cancellation the rules don't allow: the session was already marked by the aapta, or already cancelled. */
public class BookingNotCancellableException extends ApiException {

    public BookingNotCancellableException(String detail) {
        super(ErrorCode.BOOKING_NOT_CANCELLABLE, detail);
    }
}
