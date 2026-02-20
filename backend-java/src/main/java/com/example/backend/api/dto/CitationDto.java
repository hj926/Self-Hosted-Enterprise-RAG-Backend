package com.example.backend.api.dto;

public class CitationDto {
  public String docId;
  public String filename;
  public int page;
  public String snippet;

  public CitationDto(String docId, String filename, int page, String snippet) {
    this.docId = docId;
    this.filename = filename;
    this.page = page;
    this.snippet = snippet;
  }
}
