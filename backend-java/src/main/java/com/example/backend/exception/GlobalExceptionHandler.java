package com.example.backend.exception;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private String getRequestId(HttpServletRequest req) {
    Object rid = req.getAttribute("requestId");
    if (rid != null)
      return String.valueOf(rid);
    String header = req.getHeader("X-Request-ID");
    return header != null ? header : "";
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<?> handleApiException(ApiException ex, HttpServletRequest req) {
    String rid = getRequestId(req);
    log.warn("API error request_id={} code={} status={} msg={}", rid, ex.getErrorCode(), ex.getHttpStatus(),
        ex.getMessage());
    return ResponseEntity.status(ex.getHttpStatus())
        .body(Map.of(
            "error_code", ex.getErrorCode(),
            "message", ex.getMessage(),
            "request_id", rid));
  }

  @ExceptionHandler({
      MissingServletRequestPartException.class,
      MethodArgumentNotValidException.class,
      HttpMessageNotReadableException.class,
      HttpMediaTypeNotSupportedException.class,
      MissingRequestHeaderException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<?> handleBadRequest(Exception ex, HttpServletRequest req) {
    String rid = getRequestId(req);
    log.info("Bad request request_id={} type={} msg={}", rid, ex.getClass().getSimpleName(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of(
            "error_code", "BAD_REQUEST",
            "message", "Invalid request: " + ex.getMessage(),
            "request_id", rid));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
    String rid = getRequestId(req);
    log.info("Method not allowed request_id={} method={} supported={} path={}",
        rid, ex.getMethod(), ex.getSupportedHttpMethods(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(Map.of(
            "error_code", "METHOD_NOT_ALLOWED",
            "message", "Method not allowed",
            "request_id", rid));
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<?> handleNotFound(NoHandlerFoundException ex, HttpServletRequest req) {
    String rid = getRequestId(req);
    log.info("Route not found request_id={} path={}", rid, req.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of(
            "error_code", "NOT_FOUND",
            "message", "Route not found",
            "request_id", rid));
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<?> handleDbError(DataAccessException ex, HttpServletRequest req) {
    String rid = getRequestId(req);
    log.error("DB error request_id={} path={}", rid, req.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(
            "error_code", "DB_ERROR",
            "message", "Database error",
            "request_id", rid));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleUnexpected(Exception ex, HttpServletRequest req) {
    String rid = getRequestId(req);
    log.error("Unhandled error request_id={} path={}", rid, req.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(
            "error_code", "INTERNAL_ERROR",
            "message", "Internal server error",
            "request_id", rid));
  }
}