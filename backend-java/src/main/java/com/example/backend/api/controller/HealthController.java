package com.example.backend.api.controller;

import java.util.Map;

import com.example.backend.ragclient.RagClient;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ "", "/api/v1" })
public class HealthController {
  private final RagClient ragClient;

  public HealthController(RagClient ragClient) {
    this.ragClient = ragClient;
  }

  @GetMapping("/dependencies")
  public Map<String, Object> deps(HttpServletRequest req) {
    ragClient.health(req);
    return Map.of("status", "ok", "rag", "ok");
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of("status", "ok");
  }
}
