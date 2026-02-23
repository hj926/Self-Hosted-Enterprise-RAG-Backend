package com.example.backend.documents.repo;

import com.example.backend.documents.entity.DocumentEntity;
import com.example.backend.documents.entity.DocumentStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {

    Optional<DocumentEntity> findByDocIdAndTenantId(String docId, String tenantId);

    Optional<DocumentEntity> findByDocIdAndTenantIdAndStatus(String docId, String tenantId, DocumentStatus status);

    Page<DocumentEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, DocumentStatus status,
            Pageable pageable);
}