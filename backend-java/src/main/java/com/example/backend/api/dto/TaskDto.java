package com.example.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class TaskDto {

  @Schema(description = "Task id", example = "task_abc123")
  public String taskId;

  @Schema(description = "Task status", example = "SUCCEEDED")
  public String status;

  @Schema(description = "Associated document id (if available)", example = "doc_123")
  public String docId;

  @Schema(description = "Error code (if failed)", example = "INGEST_FAILED")
  public String errorCode;

  @Schema(description = "Error message (if failed)")
  public String message;

  public TaskDto(String taskId, String status, String docId, String errorCode, String message) {
    this.taskId = taskId;
    this.status = status;
    this.docId = docId;
    this.errorCode = errorCode;
    this.message = message;
  }
}