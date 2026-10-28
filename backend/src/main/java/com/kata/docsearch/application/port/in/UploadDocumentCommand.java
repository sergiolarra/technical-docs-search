package com.kata.docsearch.application.port.in;

import com.kata.docsearch.domain.model.DocumentMetadata;

public record UploadDocumentCommand(DocumentMetadata metadata, String originalFilename, long size, ContentSource content) {
}