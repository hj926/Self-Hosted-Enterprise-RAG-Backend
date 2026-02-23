package com.example.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class QueryRequest {

  @Schema(description = "User question", example = "What is this document about?")
  @NotBlank
  public String question;

  @Schema(description = "Optional document id to restrict retrieval", example = "3a710ddaf213469784972645980dc411")
  public String docId;

  @Schema(description = "Top K chunks to retrieve (optional)", example = "3")
  public Integer topK;

  public QueryRequest() {
  }
}