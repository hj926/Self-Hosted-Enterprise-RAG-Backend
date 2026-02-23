package com.example.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class IngestResponse {

  @Schema(description = "Created ingest task id", example = "b2c0f3a8e3c14d2db0d5d9f9b2a1c4ef")
  public String taskId;

  public IngestResponse() {
  }

  public IngestResponse(String taskId) {
    this.taskId = taskId;
  }
}