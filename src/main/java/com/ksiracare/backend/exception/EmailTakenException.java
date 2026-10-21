package com.ksiracare.backend.exception;

/** Another account already signs in with this email. */
public class EmailTakenException extends ApiException {

    public EmailTakenException() {
        super(ErrorCode.EMAIL_TAKEN, "An account with this email already exists (it may belong to a removed aapta).");
    }
}
