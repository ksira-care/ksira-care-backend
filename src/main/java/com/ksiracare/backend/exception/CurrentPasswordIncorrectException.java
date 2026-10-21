package com.ksiracare.backend.exception;

/**
 * The current password given when changing it is wrong. Deliberately not a 401: the session is
 * fine, and clients treat 401 as "signed out".
 */
public class CurrentPasswordIncorrectException extends ApiException {

    public CurrentPasswordIncorrectException(String detail) {
        super(ErrorCode.CURRENT_PASSWORD_INCORRECT, detail);
    }
}
