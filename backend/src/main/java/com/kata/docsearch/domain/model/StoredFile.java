package com.kata.docsearch.domain.model;

public record StoredFile(String originalFilename, DocumentFormat format, String contentType, long size, String storageKey) {
}
