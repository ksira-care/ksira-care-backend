package com.ksiracare.backend.exception;

/** An hour the aapta tried to change has a session booked in it. */
public class SlotConflictException extends ApiException {

    public SlotConflictException(String detail) {
        super(ErrorCode.SLOT_BOOKED, detail);
    }
}
