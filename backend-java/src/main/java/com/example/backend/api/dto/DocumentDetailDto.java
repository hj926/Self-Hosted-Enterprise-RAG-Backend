package com.example.backend.api.dto;

public class DocumentDetailDto extends DocumentDto {
    public int chunkCount;

    public DocumentDetailDto() {
    }

    public DocumentDetailDto(String docId, String filename, String uploadedAt, int chunkCount) {
        super(docId, filename, uploadedAt);
        this.chunkCount = chunkCount;
    }
}