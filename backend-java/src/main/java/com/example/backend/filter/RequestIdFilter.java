package com.example.backend.filter;

import java.io.IOException;
import java.util.UUID;

import com.example.backend.config.AppConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
  private final AppConfig cfg;

  public RequestIdFilter(AppConfig cfg) {
    this.cfg = cfg;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String header = cfg.getRequestIdHeader();
    String requestId = request.getHeader(header);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString().replace("-", "");
    }

    request.setAttribute("requestId", requestId);
    response.setHeader(header, requestId);
    filterChain.doFilter(request, response);
  }
}
