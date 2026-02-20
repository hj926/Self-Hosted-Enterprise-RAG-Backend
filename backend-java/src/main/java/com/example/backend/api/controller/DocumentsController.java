package com.example.backend.api.controller;

import com.example.backend.api.dto.IngestResponse;
import com.example.backend.tasks.service.IngestTaskService;
import com.example.backend.security.TenantContext;

import jakarta.validation.constraints.NotNull;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({ "", "/api/v1" })
public class DocumentsController {

  private final IngestTaskService tasks;

  public DocumentsController(IngestTaskService tasks) {
    this.tasks = tasks;
  }

  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public IngestResponse upload(@RequestPart("file") @NotNull MultipartFile file) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new com.example.backend.exception.ApiException("UNAUTHORIZED", "Missing tenant context", 401);
    }
    String taskId = tasks.createTask(tenantId, file);
    return new IngestResponse(taskId);
  }
}
