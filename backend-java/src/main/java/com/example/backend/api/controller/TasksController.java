package com.example.backend.api.controller;

import com.example.backend.api.dto.TaskDto;
import com.example.backend.exception.ApiException;
import com.example.backend.security.TenantContext;
import com.example.backend.tasks.entity.IngestTaskEntity;
import com.example.backend.tasks.service.IngestTaskService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TasksController {

  private final IngestTaskService tasks;

  public TasksController(IngestTaskService tasks) {
    this.tasks = tasks;
  }

  @GetMapping("/tasks/{taskId}")
  public TaskDto get(@PathVariable("taskId") String taskId) {
    IngestTaskEntity t = tasks.get(taskId);

    String currentTenant = TenantContext.getTenantId();
    if (currentTenant == null || currentTenant.isBlank()) {
      throw new ApiException("UNAUTHORIZED", "Missing tenant id", 401);
    }

    if (t == null || t.getTenantId() == null || !currentTenant.equals(t.getTenantId())) {
      throw new ApiException("TASK_NOT_FOUND", "Task not found", 404);
    }

    return new TaskDto(t.getTaskId(), t.getStatus().name(), t.getDocId(), t.getErrorCode(), t.getMessage());
  }
}