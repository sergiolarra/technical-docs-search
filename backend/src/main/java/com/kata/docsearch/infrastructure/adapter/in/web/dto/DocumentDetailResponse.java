package com.kata.docsearch.infrastructure.adapter.in.web.dto;

import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.DocumentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentDetailResponse(
        UUID id, String title, String author, String category, List<String> tags, String version,
        String originalFilename, DocumentFormat format, String contentType, long size,
        DocumentStatus status, String errorMessage, String content, Instant createdAt, Instant updatedAt) {

    public static DocumentDetailResponse from(Document d) {
        var m = d.getMetadata();
        var f = d.getFile();
        return new DocumentDetailResponse(d.getId(), m.title(), m.author(), m.category(), m.tags(), m.version(),
                f.originalFilename(), f.format(), f.contentType(), f.size(),
                d.getStatus(), d.getErrorMessage(), d.getContent(), d.getCreatedAt(), d.getUpdatedAt());
    }
}