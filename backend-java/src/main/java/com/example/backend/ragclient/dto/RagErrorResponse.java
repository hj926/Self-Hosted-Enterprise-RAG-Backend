package com.example.backend.ragclient.dto;

import java.util.Map;

public class RagErrorResponse {
  public String error_code;
  public String message;
  public Map<String, Object> details;
  public String request_id;
}
