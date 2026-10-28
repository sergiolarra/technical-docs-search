package com.kata.docsearch.infrastructure.adapter.in.web.dto;

import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record UploadResponse(UUID id, String title, String originalFilename, DocumentStatus status, Instant createdAt) {

    public static UploadResponse from(Document document) {
        return new UploadResponse(document.getId(), document.getMetadata().title(),
                document.getFile().originalFilename(), document.getStatus(), document.getCreatedAt());
    }
}