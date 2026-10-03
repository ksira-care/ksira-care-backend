package com.ksiracare.backend.exception;

/** The account exists but an admin has deactivated it. */
public class AccountDisabledException extends ApiException {

    public AccountDisabledException(String detail) {
        super(ErrorCode.ACCOUNT_DISABLED, detail);
    }
}
