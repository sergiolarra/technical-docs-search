package com.kata.docsearch.domain.model;

import java.util.List;
import java.util.Locale;

/** Single place for tag normalization, so stored tags and search filters always match. */
public final class Tags {

    private Tags() {
    }

    public static List<String> normalize(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}