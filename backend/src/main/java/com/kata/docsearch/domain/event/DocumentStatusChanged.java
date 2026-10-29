package com.kata.docsearch.domain.event;

import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

/** Emitted when a document leaves PROCESANDO. It is what browsers receive over SSE. */
public record DocumentStatusChanged(UUID documentId, String title, DocumentStatus status, String errorMessage,
                                    Instant occurredAt) {

    public static DocumentStatusChanged from(Document document) {
        return new DocumentStatusChanged(document.getId(), document.getMetadata().title(), document.getStatus(),
                document.getErrorMessage(), document.getUpdatedAt());
    }
}