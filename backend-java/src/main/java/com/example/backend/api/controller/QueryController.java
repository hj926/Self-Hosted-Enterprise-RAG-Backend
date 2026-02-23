package com.example.backend.api.controller;

import java.util.stream.Collectors;

import com.example.backend.api.dto.CitationDto;
import com.example.backend.api.dto.ErrorResponse;
import com.example.backend.api.dto.QueryRequest;
import com.example.backend.api.dto.QueryResponse;
import com.example.backend.audit.service.AuditService;
import com.example.backend.exception.ApiException;
import com.example.backend.ragclient.RagClient;
import com.example.backend.ragclient.dto.RagQueryRequest;
import com.example.backend.ragclient.dto.RagQueryResponse;
import com.example.backend.security.TenantContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ "", "/api/v1" })
@Tag(name = "Query", description = "RAG query endpoint")
public class QueryController {

  private final RagClient ragClient;
  private final AuditService audit;

  public QueryController(RagClient ragClient, AuditService audit) {
    this.ragClient = ragClient;
    this.audit = audit;
  }

  @Operation(summary = "Ask a question (RAG)", description = "Runs retrieval-augmented generation. Optionally restrict retrieval to a specific docId. "
      + "Returns an answer plus citations/snippets used to ground the response.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = QueryResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid API key", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public QueryResponse query(@Valid @RequestBody QueryRequest body, HttpServletRequest req) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new ApiException("UNAUTHORIZED", "Missing tenant context", 401);
    }

    RagQueryResponse res = ragClient.query(req, new RagQueryRequest(body.question, body.docId, body.topK));

    var citations = res.citations == null ? java.util.List.<CitationDto>of()
        : res.citations.stream()
            .map(c -> new CitationDto(c.doc_id, c.filename, c.page, c.snippet))
            .collect(Collectors.toList());

    audit.logQuery(tenantId, body.docId, body.question, res.retrieved_count);
    return new QueryResponse(res.answer, citations, res.retrieved_count);
  }
}