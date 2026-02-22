package com.example.backend.api.controller;

import com.example.backend.api.dto.DocumentDto;
import com.example.backend.api.dto.DocumentDetailDto;
import com.example.backend.api.dto.IngestResponse;
import com.example.backend.ragclient.RagClient;
import com.example.backend.security.TenantContext;
import com.example.backend.tasks.service.IngestTaskService;
import com.example.backend.exception.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({ "", "/api/v1" })
public class DocumentsController {

  private final IngestTaskService tasks;
  private final RagClient rag;

  public DocumentsController(IngestTaskService tasks, RagClient rag) {
    this.tasks = tasks;
    this.rag = rag;
  }

  /**
   * 辅助方法：校验租户上下文
   */
  private void checkTenant() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new ApiException("UNAUTHORIZED", "Missing tenant context", 401);
    }
  }

  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public IngestResponse upload(@RequestPart("file") @NotNull MultipartFile file) {
    checkTenant();
    String taskId = tasks.createTask(TenantContext.getTenantId(), file);
    return new IngestResponse(taskId);
  }

  @GetMapping("/documents")
  public List<DocumentDto> list(HttpServletRequest req) {
    checkTenant();

    return rag.listDocuments(req).stream()
        .map(d -> new DocumentDto(d.doc_id, d.filename, d.uploaded_at))
        .collect(Collectors.toList());
  }

  @GetMapping("/documents/{docId}")
  public DocumentDetailDto get(@PathVariable("docId") String docId, HttpServletRequest req) {
    checkTenant();

    var d = rag.getDocument(req, docId);
    return new DocumentDetailDto(d.doc_id, d.filename, d.uploaded_at, d.chunk_count);
  }

  @DeleteMapping("/documents/{docId}")
  public DocumentDto delete(@PathVariable("docId") String docId, HttpServletRequest req) {
    checkTenant();

    var d = rag.deleteDocument(req, docId);
    return new DocumentDto(d.doc_id, d.filename, d.uploaded_at);
  }
}