package com.example.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class DocumentDto {

    @Schema(description = "Document id", example = "doc_123")
    public String docId;

    @Schema(description = "Original filename", example = "resume.pdf")
    public String filename;

    @Schema(description = "Document status", example = "READY")
    public String status;

    @Schema(description = "Created time string", example = "2026-02-22T04:40:29Z")
    public String createdAt;

    public DocumentDto() {
    }

    public DocumentDto(String docId, String filename, String status, String createdAt) {
        this.docId = docId;
        this.filename = filename;
        this.status = status;
        this.createdAt = createdAt;
    }
}