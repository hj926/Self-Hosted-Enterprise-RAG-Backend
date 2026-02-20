package com.example.backend.exception;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({
      MissingServletRequestPartException.class,
      MethodArgumentNotValidException.class,
      HttpMessageNotReadableException.class,
      HttpMediaTypeNotSupportedException.class
  })
  public ResponseEntity<?> handleBadRequest(Exception ex, HttpServletRequest req) {
    String requestId = (String) req.getAttribute("requestId");
    String msg = ex.getMessage() == null ? "Bad request" : ex.getMessage();
    return ResponseEntity.status(400)
        .body(Map.of(
            "error_code", "BAD_REQUEST",
            "message", msg,
            "request_id", requestId));
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<?> handle(ApiException ex, HttpServletRequest req) {
    String requestId = (String) req.getAttribute("requestId");
    return ResponseEntity.status(ex.getHttpStatus())
        .body(Map.of(
            "error_code", ex.getErrorCode(),
            "message", ex.getMessage(),
            "request_id", requestId));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleAny(Exception ex, HttpServletRequest req) {
    String requestId = (String) req.getAttribute("requestId");
    return ResponseEntity.status(500)
        .body(Map.of(
            "error_code", "INTERNAL_ERROR",
            "message", "Internal server error",
            "request_id", requestId));
  }
}
