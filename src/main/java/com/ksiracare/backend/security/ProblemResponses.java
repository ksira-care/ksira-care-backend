package com.ksiracare.backend.security;

import com.ksiracare.backend.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Writes the same problem format as GlobalExceptionHandler for failures that happen in the
 * security filters, before any controller runs.
 */
final class ProblemResponses {

    private ProblemResponses() {
    }

    static void write(HttpServletResponse response, ErrorCode code, String detail) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        // Only constant, trusted strings reach here, so no JSON escaping is needed.
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","code":"%s"}"""
                .formatted(code.status().getReasonPhrase(), code.status().value(), detail, code.name()));
    }
}
