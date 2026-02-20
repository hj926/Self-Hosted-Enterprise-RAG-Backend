package com.example.backend.api.controller;

import java.util.stream.Collectors;

import com.example.backend.api.dto.*;
import com.example.backend.audit.service.AuditService;
import com.example.backend.ragclient.RagClient;
import com.example.backend.ragclient.dto.RagQueryRequest;
import com.example.backend.ragclient.dto.RagQueryResponse;
import com.example.backend.security.TenantContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ "", "/api/v1" })
public class QueryController {

  private final RagClient ragClient;
  private final AuditService audit;

  public QueryController(RagClient ragClient, AuditService audit) {
    this.ragClient = ragClient;
    this.audit = audit;
  }

  @PostMapping("/query")
  public QueryResponse query(@Valid @RequestBody QueryRequest body, HttpServletRequest req) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new com.example.backend.exception.ApiException("UNAUTHORIZED", "Missing tenant context", 401);
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
