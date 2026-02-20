package com.example.backend.audit.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "action", nullable = false, length = 32)
  private String action;

  @Column(name = "doc_id", length = 64)
  private String docId;

  @Column(name = "question", length = 512)
  private String question;

  @Column(name = "retrieved_count")
  private Integer retrievedCount;

  public Long getId() { return id; }

  public String getTenantId() { return tenantId; }
  public void setTenantId(String tenantId) { this.tenantId = tenantId; }

  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }

  public String getDocId() { return docId; }
  public void setDocId(String docId) { this.docId = docId; }

  public String getQuestion() { return question; }
  public void setQuestion(String question) { this.question = question; }

  public Integer getRetrievedCount() { return retrievedCount; }
  public void setRetrievedCount(Integer retrievedCount) { this.retrievedCount = retrievedCount; }
}
