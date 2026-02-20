package com.example.backend.api.dto;

import java.util.List;

public class QueryResponse {
  public String answer;
  public List<CitationDto> citations;
  public int retrievedCount;

  public QueryResponse(String answer, List<CitationDto> citations, int retrievedCount) {
    this.answer = answer;
    this.citations = citations;
    this.retrievedCount = retrievedCount;
  }
}
