package com.example.backend.api.controller;

import com.example.backend.config.AppConfig;
import com.example.backend.ragclient.RagClient;

import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Health checks (gateway + downstream rag-service)")
public class HealthController {

  private final RagClient rag;
  private final AppConfig cfg;

  public HealthController(RagClient rag, AppConfig cfg) {
    this.rag = rag;
    this.cfg = cfg;
  }

  @Operation(summary = "Health check", description = "Returns gateway status and downstream rag-service health. Use deep=true for deep checks.")
  @GetMapping("/health")
  public Map<String, Object> health(
      HttpServletRequest request,
      @RequestParam(name = "deep", required = false, defaultValue = "false") boolean deep) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("status", "ok");

    try {
      Map<String, Object> ragHealth = (deep ? rag.healthDeep(request) : rag.healthQuick(request)).block();
      out.put("rag", ragHealth);

      if (ragHealth != null && "degraded".equals(String.valueOf(ragHealth.get("status")))) {
        out.put("status", "degraded");
      }
    } catch (Exception e) {
      Map<String, Object> ragDown = new LinkedHashMap<>();
      ragDown.put("status", "unavailable");
      ragDown.put("error", e.getMessage());
      ragDown.put("mode", deep ? "deep" : "quick");
      ragDown.put("ragBaseUrl", cfg.getRagBaseUrl());
      out.put("rag", ragDown);
      out.put("status", "degraded");
    }

    return out;
  }
}