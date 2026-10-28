package com.kata.docsearch.domain.model;

import com.kata.docsearch.domain.exception.InvalidDocumentStateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Document {
    private final UUID id;
    private final DocumentMetadata metadata;
    private final StoredFile file;
    private final Instant createdAt;
    private DocumentStatus status;
    private String content;
    private String errorMessage;
    private Instant updatedAt;

    private Document(UUID id, DocumentMetadata metadata, StoredFile file, DocumentStatus status,
                     String content, String errorMessage, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.metadata = Objects.requireNonNull(metadata);
        this.file = Objects.requireNonNull(file);
        this.status = Objects.requireNonNull(status);
        this.content = content;
        this.errorMessage = errorMessage;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Document newPending(UUID id, DocumentMetadata metadata, StoredFile file, Instant now) {
        return new Document(id, metadata, file, DocumentStatus.PROCESANDO, null, null, now, now);
    }

    /** Reconstruye un documento ya persistido (lo usa el adaptador de persistencia). */
    public static Document restore(UUID id, DocumentMetadata metadata, StoredFile file, DocumentStatus status,
                                   String content, String errorMessage, Instant createdAt, Instant updatedAt) {
        return new Document(id, metadata, file, status, content, errorMessage, createdAt, updatedAt);
    }

    public void markIndexed(String extractedContent, Instant now) {
        ensureProcessing();
        this.content = extractedContent;
        this.errorMessage = null;
        this.status = DocumentStatus.INDEXADO;
        this.updatedAt = now;
    }

    public void markFailed(String reason, Instant now) {
        ensureProcessing();
        this.errorMessage = reason;
        this.status = DocumentStatus.ERROR;
        this.updatedAt = now;
    }

    public boolean isProcessing() {
        return status == DocumentStatus.PROCESANDO;
    }

    private void ensureProcessing() {
        if (!isProcessing()) {
            throw new InvalidDocumentStateException(id, status);
        }
    }

    public UUID getId() { return id; }
    public DocumentMetadata getMetadata() { return metadata; }
    public StoredFile getFile() { return file; }
    public DocumentStatus getStatus() { return status; }
    public String getContent() { return content; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

}
