package com.example.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CitationDto {

  @Schema(description = "Document id", example = "3a710ddaf213469784972645980dc411")
  public String docId;

  @Schema(description = "Original file name", example = "sample.pdf")
  public String filename;

  @Schema(description = "Page number (1-based)", example = "2")
  public int page;

  @Schema(description = "Supporting snippet", example = "This section describes ...")
  public String snippet;

  public CitationDto() {
  }

  public CitationDto(String docId, String filename, int page, String snippet) {
    this.docId = docId;
    this.filename = filename;
    this.page = page;
    this.snippet = snippet;
  }
}