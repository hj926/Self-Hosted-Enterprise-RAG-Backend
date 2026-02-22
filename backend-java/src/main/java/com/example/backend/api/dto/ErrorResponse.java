package com.example.backend.api.dto;

public class ErrorResponse {
    public String request_id;
    public String error_code;
    public String message;

    public ErrorResponse() {
    }

    public ErrorResponse(String requestId, String errorCode, String message) {
        this.request_id = requestId;
        this.error_code = errorCode;
        this.message = message;
    }
}