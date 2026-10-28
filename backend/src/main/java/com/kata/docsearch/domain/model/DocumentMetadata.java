package com.kata.docsearch.domain.model;

import com.kata.docsearch.domain.exception.InvalidDocumentException;

import java.util.List;
import java.util.Locale;

public record DocumentMetadata(String title, String author, String category, List<String> tags, String version) {

    public DocumentMetadata {
        title = requireText(title, "title");
        author = requireText(author, "author");
        category = requireText(category, "category");
        version = requireText(version, "version");
        tags = tags == null ? List.of() : tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidDocumentException("The field '" + field + "' is required");
        }
        return value.trim();
    }
}
