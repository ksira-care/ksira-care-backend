package com.ksiracare.backend.exception;

/** Wrong email or password. Deliberately doesn't say which, so it can't reveal who has an account. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String detail) {
        super(ErrorCode.INVALID_CREDENTIALS, detail);
    }
}
