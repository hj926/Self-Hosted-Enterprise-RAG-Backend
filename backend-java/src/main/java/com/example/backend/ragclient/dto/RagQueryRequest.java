package com.example.backend.ragclient.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagQueryRequest {

  @JsonProperty("question")
  private String question;

  @JsonProperty("doc_id")
  private String docId;

  @JsonProperty("top_k")
  private Integer topK;

  public RagQueryRequest() {
  }

  public RagQueryRequest(String question, String docId, Integer topK) {
    this.question = question;
    this.docId = docId;
    this.topK = topK;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public String getDocId() {
    return docId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }

  public Integer getTopK() {
    return topK;
  }

  public void setTopK(Integer topK) {
    this.topK = topK;
  }
}