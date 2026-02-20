package com.example.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

public class QueryRequest {
  @NotBlank
  public String question;

  public String docId;
  public Integer topK;
}
