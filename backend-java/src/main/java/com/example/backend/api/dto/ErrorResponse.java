package com.example.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ErrorResponse {

    @Schema(description = "Request id for debugging", example = "74e8612aea8f48398b272e8832658b91")
    public String request_id;

    @Schema(description = "Stable error code", example = "UNAUTHORIZED")
    public String error_code;

    @Schema(description = "Human-readable message", example = "Missing API key")
    public String message;

    public ErrorResponse() {
    }

    public ErrorResponse(String requestId, String errorCode, String message) {
        this.request_id = requestId;
        this.error_code = errorCode;
        this.message = message;
    }
}