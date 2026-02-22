package com.example.backend.ragclient;

import com.example.backend.config.AppConfig;
import com.example.backend.exception.ApiException;
import com.example.backend.ragclient.dto.RagDocumentDetailOut;
import com.example.backend.ragclient.dto.RagDocumentOut;
import com.example.backend.ragclient.dto.RagErrorResponse;
import com.example.backend.ragclient.dto.RagQueryRequest;
import com.example.backend.ragclient.dto.RagQueryResponse;
import com.example.backend.security.TenantContext;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class RagClient {
  private final WebClient webClient;
  private final AppConfig cfg;

  public RagClient(WebClient ragWebClient, AppConfig cfg) {
    this.webClient = ragWebClient;
    this.cfg = cfg;
  }

  private String getRequestId(HttpServletRequest req) {
    return (String) req.getAttribute("requestId");
  }

  private String getTenantId() {
    return TenantContext.getTenantId();
  }

  public Mono<Map<String, Object>> health(HttpServletRequest req) {
    return healthQuick(req);
  }

  public Mono<Map<String, Object>> healthQuick(HttpServletRequest req) {
    return webClient.get()
        .uri("/health")
        .header(cfg.getRequestIdHeader(), getRequestId(req))
        .header(cfg.getTenantHeader(), getTenantId())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .timeout(Duration.ofMillis(1500));
  }

  public Mono<Map<String, Object>> healthDeep(HttpServletRequest req) {
    return webClient.get()
        .uri("/health/deep")
        .header(cfg.getRequestIdHeader(), getRequestId(req))
        .header(cfg.getTenantHeader(), getTenantId())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .timeout(Duration.ofSeconds(12));
  }

  public List<RagDocumentOut> listDocuments(HttpServletRequest req) {
    try {
      return webClient.get()
          .uri("/documents")
          .header(cfg.getRequestIdHeader(), getRequestId(req))
          .header(cfg.getTenantHeader(), getTenantId())
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .onStatus(status -> status.isError(),
              r -> r.bodyToMono(RagErrorResponse.class)
                  .defaultIfEmpty(new RagErrorResponse())
                  .flatMap(er -> Mono.error(new ApiException(
                      er.error_code != null ? er.error_code : "RAG_ERROR",
                      er.message != null ? er.message : ("RAG request failed: status=" + r.statusCode().value()),
                      r.statusCode().value()))))
          .bodyToFlux(RagDocumentOut.class)
          .collectList()
          .block();
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException("RAG_ERROR", "Failed to call RAG service", 502);
    }
  }

  public RagDocumentDetailOut getDocument(HttpServletRequest req, String docId) {
    try {
      return webClient.get()
          .uri("/documents/{docId}", docId)
          .header(cfg.getRequestIdHeader(), getRequestId(req))
          .header(cfg.getTenantHeader(), getTenantId())
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .onStatus(status -> status.isError(),
              r -> r.bodyToMono(RagErrorResponse.class)
                  .defaultIfEmpty(new RagErrorResponse())
                  .flatMap(er -> Mono.error(new ApiException(
                      er.error_code != null ? er.error_code : "RAG_ERROR",
                      er.message != null ? er.message : ("RAG request failed: status=" + r.statusCode().value()),
                      r.statusCode().value()))))
          .bodyToMono(RagDocumentDetailOut.class)
          .block();
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException("RAG_ERROR", "Failed to call RAG service", 502);
    }
  }

  public RagDocumentOut deleteDocument(HttpServletRequest req, String docId) {
    try {
      return webClient.delete()
          .uri("/documents/{docId}", docId)
          .header(cfg.getRequestIdHeader(), getRequestId(req))
          .header(cfg.getTenantHeader(), getTenantId())
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .onStatus(status -> status.isError(),
              r -> r.bodyToMono(RagErrorResponse.class)
                  .defaultIfEmpty(new RagErrorResponse())
                  .flatMap(er -> Mono.error(new ApiException(
                      er.error_code != null ? er.error_code : "RAG_ERROR",
                      er.message != null ? er.message : ("RAG request failed: status=" + r.statusCode().value()),
                      r.statusCode().value()))))
          .bodyToMono(RagDocumentOut.class)
          .block();
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException("RAG_ERROR", "Failed to call RAG service", 502);
    }
  }

  public RagQueryResponse query(HttpServletRequest req, RagQueryRequest body) {
    if (body == null) {
      throw new ApiException("BAD_REQUEST", "Query body is required", 400);
    }

    try {
      return webClient.post()
          .uri("/query")
          .header(cfg.getRequestIdHeader(), getRequestId(req))
          .header(cfg.getTenantHeader(), getTenantId())
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