package com.example.backend.audit.service;

import com.example.backend.audit.entity.AuditLogEntity;
import com.example.backend.audit.repo.AuditLogRepository;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

  private final AuditLogRepository repo;

  public AuditService(AuditLogRepository repo) {
    this.repo = repo;
  }

  public void logQuery(String tenantId, String docId, String question, int retrievedCount) {
    AuditLogEntity a = new AuditLogEntity();
    a.setTenantId(tenantId);
    a.setAction("QUERY");
    a.setDocId(docId);
    a.setQuestion(question);
    a.setRetrievedCount(retrievedCount);
    repo.save(a);
  }
}
