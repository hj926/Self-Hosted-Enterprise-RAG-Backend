package com.example.backend.tasks.service;

import java.util.UUID;

import com.example.backend.exception.ApiException;
import com.example.backend.tasks.entity.IngestTaskEntity;
import com.example.backend.tasks.repo.IngestTaskRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IngestTaskService {

  private static final Logger log = LoggerFactory.getLogger(IngestTaskService.class);

  private final IngestTaskRepository repo;

  public IngestTaskService(IngestTaskRepository repo) {
    this.repo = repo;
  }

  public String createTask(String tenantId, MultipartFile file) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new ApiException("UNAUTHORIZED", "Missing tenant id", HttpStatus.UNAUTHORIZED.value());
    }
    if (file == null || file.isEmpty()) {
      throw new ApiException("BAD_REQUEST", "Missing file", HttpStatus.BAD_REQUEST.value());
    }

    try {
      IngestTaskEntity t = new IngestTaskEntity();
      t.setTaskId(UUID.randomUUID().toString().replace("-", ""));
      t.setTenantId(tenantId);
      t.setStatus(IngestTaskEntity.Status.PENDING);
      t.setFilename(file.getOriginalFilename() == null ? "uploaded.pdf" : file.getOriginalFilename());
      t.setFileBytes(file.getBytes());
      repo.save(t);
      return t.getTaskId();
    } catch (Exception e) {
      log.error("Failed to create ingest task", e);
      throw new ApiException("INGEST_TASK_CREATE_FAILED", "Failed to create ingest task",
          HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
  }

  public IngestTaskEntity get(String taskId) {
    return repo.findById(taskId)
        .orElseThrow(() -> new ApiException("TASK_NOT_FOUND", "Task not found", 404));
  }

  public void save(IngestTaskEntity t) {
    repo.save(t);
  }
}
