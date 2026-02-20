package com.example.backend.tasks.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ingest_tasks")
public class IngestTaskEntity {

  public enum Status {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
  }

  @Id
  @Column(name = "task_id", nullable = false, length = 64)
  private String taskId;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Lob
  @Column(name = "file_bytes", nullable = false)
  private byte[] fileBytes;

  @Column(name = "filename", nullable = false, length = 256)
  private String filename;

  @Column(name = "doc_id", length = 64)
  private String docId;

  @Column(name = "error_code", length = 64)
  private String errorCode;

  @Column(name = "message", length = 512)
  private String message;

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public byte[] getFileBytes() {
    return fileBytes;
  }

  public void setFileBytes(byte[] fileBytes) {
    this.fileBytes = fileBytes;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getDocId() {
    return docId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Transient
  public Status getStatus() {
    if (status == null || status.isBlank())
      return Status.PENDING;
    return Status.valueOf(status);
  }

  public void setStatus(Status s) {
    this.status = (s == null) ? Status.PENDING.name() : s.name();
  }

  public String getStatusValue() {
    return status;
  }

  public void setStatusValue(String status) {
    this.status = status;
  }
}
