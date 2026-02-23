package com.example.backend.documents.service;

import com.example.backend.documents.entity.DocumentEntity;
import com.example.backend.documents.entity.DocumentStatus;
import com.example.backend.documents.repo.DocumentRepository;
import com.example.backend.exception.ApiException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentRepository repo;

    public DocumentService(DocumentRepository repo) {
        this.repo = repo;
    }

    public DocumentEntity upsertReadyDocument(String tenantId, String docId, String filename, String taskId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is empty");
        }
        if (docId == null || docId.isBlank()) {
            throw new IllegalArgumentException("docId is empty");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename is empty");
        }

        Optional<DocumentEntity> existing = repo.findByDocIdAndTenantId(docId, tenantId);
        DocumentEntity d = existing.orElseGet(DocumentEntity::new);

        if (d.getCreatedAt() == null) {
            d.setCreatedAt(Instant.now());
        }

        d.setDocId(docId);
        d.setTenantId(tenantId);
        d.setFilename(filename);
        d.setStatus(DocumentStatus.READY);
        d.setDeletedAt(null);
        d.setLastIngestTaskId(taskId);

        return repo.save(d);
    }

    public Page<DocumentEntity> listActive(String tenantId, Pageable pageable) {
        return repo.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, DocumentStatus.READY, pageable);
    }

    public Optional<DocumentEntity> findActive(String tenantId, String docId) {
        return repo.findByDocIdAndTenantIdAndStatus(docId, tenantId, DocumentStatus.READY);
    }

    public DocumentEntity getActiveOrThrow(String tenantId, String docId) {
        return findActive(tenantId, docId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Document not found", 404));
    }

    public void softDeleteOrThrow(String tenantId, String docId) {
        DocumentEntity d = repo.findByDocIdAndTenantId(docId, tenantId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Document not found", 404));

        if (d.getStatus() != DocumentStatus.DELETED) {
            d.setStatus(DocumentStatus.DELETED);
            d.setDeletedAt(Instant.now());
            repo.save(d);
        }
    }
}