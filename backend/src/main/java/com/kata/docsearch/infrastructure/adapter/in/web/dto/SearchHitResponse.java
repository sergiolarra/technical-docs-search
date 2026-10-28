package com.kata.docsearch.infrastructure.adapter.in.web.dto;

import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.SearchHit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SearchHitResponse(
        UUID id,
        String title,
        List<FragmentResponse> titleHighlight,
        String author,
        String category,
        List<String> tags,
        String version,
        DocumentFormat format,
        double score,
        List<FragmentResponse> snippet,
        Instant createdAt) {

    public static SearchHitResponse from(SearchHit hit) {
        return new SearchHitResponse(
                hit.id(),
                hit.title().plainText(),
                FragmentResponse.from(hit.title()),
                hit.author(),
                hit.category(),
                hit.tags(),
                hit.version(),
                hit.format(),
                Math.round(hit.score() * 10_000) / 10_000.0,
                FragmentResponse.from(hit.snippet()),
                hit.createdAt());
    }
}