package com.ksiracare.backend.exception;

/** An edit that includes fields the caller isn't allowed to change. */
public class FieldNotEditableException extends ApiException {

    public FieldNotEditableException(String detail) {
        super(ErrorCode.FIELD_NOT_EDITABLE, detail);
    }
}
