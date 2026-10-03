package com.ksiracare.backend.security;

import com.ksiracare.backend.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/** Answers unauthenticated requests with 401 (not Spring's default 403) in the API's problem format. */
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorCode code;
    private final String detail;

    public ProblemAuthenticationEntryPoint(ErrorCode code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        ProblemResponses.write(response, code, detail);
    }
}
