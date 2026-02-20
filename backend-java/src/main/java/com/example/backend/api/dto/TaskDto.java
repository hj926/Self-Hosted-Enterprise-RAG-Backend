package com.example.backend.api.dto;

public class TaskDto {
  public String taskId;
  public String status;
  public String docId;
  public String errorCode;
  public String message;

  public TaskDto(String taskId, String status, String docId, String errorCode, String message) {
    this.taskId = taskId;
    this.status = status;
    this.docId = docId;
    this.errorCode = errorCode;
    this.message = message;
  }
}
