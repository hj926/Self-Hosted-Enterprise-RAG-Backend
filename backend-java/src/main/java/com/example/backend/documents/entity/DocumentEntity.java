package com.example.backend.documents.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_tenant_created", columnList = "tenant_id,created_at"),
        @Index(name = "idx_documents_tenant_status", columnList = "tenant_id,status")
})
public class DocumentEntity {

    @Id
    @Column(name = "doc_id", nullable = false, length = 64)
    private String docId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "filename", nullable = false, length = 256)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DocumentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "last_ingest_task_id", length = 64)
    private String lastIngestTaskId;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getLastIngestTaskId() {
        return lastIngestTaskId;
    }

    public void setLastIngestTaskId(String lastIngestTaskId) {
        this.lastIngestTaskId = lastIngestTaskId;
    }
}