package com.kata.docsearch.infrastructure.adapter.in.web.dto;

import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentStatus;

import java.util.UUID;

/** Result per file: an invalid file does not prevent the others from being accepted. */
public record BatchUploadItemResponse(String originalFilename, boolean accepted, UUID id, DocumentStatus status, String error) {

    public static BatchUploadItemResponse accepted(Document document) {
        return new BatchUploadItemResponse(document.getFile().originalFilename(), true,
                document.getId(), document.getStatus(), null);
    }

    public static BatchUploadItemResponse rejected(String originalFilename, String error) {
        return new BatchUploadItemResponse(originalFilename, false, null, null, error);
    }
}