package com.lending.shared.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        List<FieldError> details,
        String path,
        String traceId
) {
    public record FieldError(String field, String message) {
    }
}
