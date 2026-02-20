package com.example.backend.ragclient;

import com.example.backend.config.AppConfig;
import com.example.backend.exception.ApiException;
import com.example.backend.ragclient.dto.RagErrorResponse;
import com.example.backend.ragclient.dto.RagQueryRequest;
import com.example.backend.ragclient.dto.RagQueryResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class RagClient {
  private final WebClient webClient;
  private final AppConfig cfg;

  public RagClient(WebClient ragWebClient, AppConfig cfg) {
    this.webClient = ragWebClient;
    this.cfg = cfg;
  }

  public void health(HttpServletRequest req) {
    String requestId = (String) req.getAttribute("requestId");
    try {
      webClient.get()
          .uri("/health")
          .header(cfg.getRequestIdHeader(), requestId)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .onStatus(status -> status.isError(),
              r -> r.bodyToMono(String.class).flatMap(
                  body -> Mono.error(new ApiException("RAG_UNAVAILABLE", "RAG service health check failed", 503))))
          .bodyToMono(String.class)
          .block();
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException("RAG_UNAVAILABLE", "RAG service is unavailable", 503);
    }
  }

  public RagQueryResponse query(HttpServletRequest req, RagQueryRequest body) {
    String requestId = (String) req.getAttribute("requestId");

    if (body == null) {
      throw new ApiException("BAD_REQUEST", "Query body is required", 400);
    }

    try {
      return webClient.post()
          .uri("/query")
          .header(cfg.getRequestIdHeader(), requestId)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(body)
          .retrieve()
          .onStatus(status -> status.isError(),
              r -> r.bodyToMono(RagErrorResponse.class)
                  .defaultIfEmpty(new RagErrorResponse())
                  .flatMap(er -> Mono.error(new ApiException(
                      er.error_code != null ? er.error_code : "RAG_ERROR",
                      er.message != null ? er.message : ("RAG request failed: status=" + r.statusCode().value()),
                      r.statusCode().value()))))
          .bodyToMono(RagQueryResponse.class)
          .block();
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException("RAG_ERROR", "Failed to call RAG service", 502);
    }
  }
}