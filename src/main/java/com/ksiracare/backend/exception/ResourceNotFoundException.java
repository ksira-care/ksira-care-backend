package com.ksiracare.backend.exception;

/** Something the client asked for doesn't exist — or isn't theirs, which is reported the same way. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String detail) {
        super(ErrorCode.NOT_FOUND, detail);
    }
}
