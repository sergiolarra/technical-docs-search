package com.kata.docsearch.domain.model;

import com.kata.docsearch.domain.exception.InvalidDocumentException;

import java.util.List;

public record DocumentMetadata(String title, String author, String category, List<String> tags, String version) {

    public DocumentMetadata {
        title = requireText(title, "title");
        author = requireText(author, "author");
        category = requireText(category, "category");
        version = requireText(version, "version");
        tags = Tags.normalize(tags);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidDocumentException("The field '" + field + "' is required");
        }
        return value.trim();
    }
}
