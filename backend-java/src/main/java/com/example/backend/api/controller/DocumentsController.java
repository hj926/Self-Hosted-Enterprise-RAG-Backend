package com.example.backend.api.controller;

import com.example.backend.api.dto.DocumentDto;
import com.example.backend.api.dto.DocumentDetailDto;
import com.example.backend.api.dto.ErrorResponse;
import com.example.backend.api.dto.IngestResponse;
import com.example.backend.documents.service.DocumentService;
import com.example.backend.exception.ApiException;
import com.example.backend.ragclient.RagClient;
import com.example.backend.security.TenantContext;
import com.example.backend.tasks.service.IngestTaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({ "", "/api/v1" })
@Tag(name = "Documents", description = "Document upload, listing, details and deletion")
public class DocumentsController {

  private final IngestTaskService tasks;
  private final DocumentService documents;
  private final RagClient rag;

  public DocumentsController(IngestTaskService tasks, DocumentService documents, RagClient rag) {
    this.tasks = tasks;
    this.documents = documents;
    this.rag = rag;
  }

  private void checkTenant() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new ApiException("UNAUTHORIZED", "Missing tenant context", 401);
    }
  }

  @Operation(summary = "Upload a document (PDF)", description = "Uploads a PDF and creates an async ingest task. Use GET /api/v1/tasks/{taskId} to track status.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Task created", content = @Content(schema = @Schema(implementation = IngestResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid API key", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public IngestResponse upload(
      @Parameter(description = "PDF file to ingest", required = true) @RequestPart("file") @NotNull MultipartFile file) {
    checkTenant();
    String taskId = tasks.createTask(TenantContext.getTenantId(), file);
    return new IngestResponse(taskId);
  }

  @Operation(summary = "List documents", description = "Lists READY documents under the current tenant (DB authoritative).")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentDto.class)))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid API key", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/documents")
  public List<DocumentDto> list(
      @Parameter(description = "Page index (0-based)", example = "0") @RequestParam(name = "page", defaultValue = "0") int page,
      @Parameter(description = "Page size", example = "20") @RequestParam(name = "size", defaultValue = "20") int size) {
    checkTenant();

    int safePage = Math.max(page, 0);
    int safeSize = Math.max(1, Math.min(size, 100));

    var p = documents.listActive(TenantContext.getTenantId(), PageRequest.of(safePage, safeSize));
    return p.stream()
        .map(d -> new DocumentDto(d.getDocId(), d.getFilename(), d.getStatus().name(), d.getCreatedAt().toString()))
        .toList();
  }

  @Operation(summary = "Get document details", description = "Returns metadata from DB and best-effort chunk count from RAG.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = DocumentDetailDto.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid API key", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Document not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/documents/{docId}")
  public DocumentDetailDto get(
      @Parameter(description = "Document id", required = true) @PathVariable("docId") String docId,
      HttpServletRequest req) {
    checkTenant();

    var d = documents.getActiveOrThrow(TenantContext.getTenantId(), docId);

    int chunkCount = 0;
    try {
      var rd = rag.getDocument(req, docId);
      chunkCount = rd.chunk_count;
    } catch (Exception ignored) {
      chunkCount = 0;
    }

    return new DocumentDetailDto(
        d.getDocId(),
        d.getFilename(),
        d.getStatus().name(),
        d.getCreatedAt().toString(),
        chunkCount,
        d.getLastIngestTaskId());
  }

  @Operation(summary = "Delete a document", description = "Soft-deletes a document in DB and best-effort deletes from RAG. After deletion, detail returns 404.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Deleted"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid API key", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Document not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @DeleteMapping("/documents/{docId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @Parameter(description = "Document id", required = true) @PathVariable("docId") String docId,
      HttpServletRequest req) {
    checkTenant();

    documents.softDeleteOrThrow(TenantContext.getTenantId(), docId);

    try {
      rag.deleteDocument(req, docId);
    } catch (Exception ignored) {
    }
  }
}