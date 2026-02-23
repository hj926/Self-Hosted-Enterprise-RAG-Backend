package com.example.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class DocumentDetailDto extends DocumentDto {

    @Schema(description = "Number of indexed chunks (best-effort)", example = "128")
    public int chunkCount;

    @Schema(description = "Last ingest task id", example = "task_abc")
    public String lastIngestTaskId;

    public DocumentDetailDto() {
    }

    public DocumentDetailDto(
            String docId,
            String filename,
            String status,
            String createdAt,
            int chunkCount,
            String lastIngestTaskId) {
        super(docId, filename, status, createdAt);
        this.chunkCount = chunkCount;
        this.lastIngestTaskId = lastIngestTaskId;
    }
}