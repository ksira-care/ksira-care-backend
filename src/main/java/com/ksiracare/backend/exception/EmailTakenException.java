package com.ksiracare.backend.exception;

/** Another account already signs in with this email. */
public class EmailTakenException extends ApiException {

    public EmailTakenException(String detail) {
        super(ErrorCode.EMAIL_TAKEN, detail);
    }
}
