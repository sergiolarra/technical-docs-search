package com.kata.docsearch.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SearchHit(
        UUID id,
        HighlightedText title,
        String author,
        String category,
        List<String> tags,
        String version,
        DocumentFormat format,
        double score,
        HighlightedText snippet,
        Instant createdAt) {
}