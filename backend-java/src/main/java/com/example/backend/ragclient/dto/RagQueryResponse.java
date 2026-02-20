package com.example.backend.ragclient.dto;

import java.util.List;
import java.util.Map;

public class RagQueryResponse {
  public String answer;
  public List<RagCitation> citations;
  public int retrieved_count;
  public Map<String, Double> timings;
}
