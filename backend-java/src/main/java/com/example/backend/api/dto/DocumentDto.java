package com.example.backend.api.dto;

public class DocumentDto {
    public String docId;
    public String filename;
    public String uploadedAt;

    public DocumentDto() {
    }

    public DocumentDto(String docId, String filename, String uploadedAt) {
        this.docId = docId;
        this.filename = filename;
        this.uploadedAt = uploadedAt;
    }
}