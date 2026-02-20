package com.example.backend.security;

import java.io.IOException;
import java.util.Optional;

import com.example.backend.config.AppConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

  private final AppConfig cfg;

  public ApiKeyAuthFilter(AppConfig cfg) {
    this.cfg = cfg;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null)
      return false;
    return path.startsWith("/actuator/") || path.equals("/health") || path.equals("/api/v1/health");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String header = cfg.getApiKeyHeader();
    String key = request.getHeader(header);

    if (key == null || key.isBlank()) {
      writeJson(response, 401, "{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing API key\"}");
      return;
    }

    Optional<AppConfig.ApiKeyEntry> entry = cfg.getAuth().getKeys().stream()
        .filter(e -> key.equals(e.getKey()))
        .findFirst();

    if (entry.isEmpty()) {
      writeJson(response, 401, "{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid API key\"}");
      return;
    }

    try {
      TenantContext.setTenantId(entry.get().getTenantId());
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private static void writeJson(HttpServletResponse response, int status, String body) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(body);
  }
}
