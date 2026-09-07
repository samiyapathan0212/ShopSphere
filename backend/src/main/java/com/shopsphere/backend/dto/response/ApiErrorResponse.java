package com.shopsphere.backend.dto.response;

import java.util.Map;

/**
 * Standard error envelope returned by the centralized exception handler.
 */
public record ApiErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(status, error, message, null);
    }

    public static ApiErrorResponse of(int status, String error, String message, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(status, error, message, fieldErrors);
    }
}