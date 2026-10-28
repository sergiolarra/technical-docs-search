package com.kata.docsearch.infrastructure.adapter.in.web.dto;

import com.kata.docsearch.domain.model.SearchQuery;
import com.kata.docsearch.domain.model.SearchResultPage;

import java.util.List;

public record SearchResponse(
        String query,
        int page,
        int size,
        long totalHits,
        int totalPages,
        double tookMs,
        List<SearchHitResponse> results) {

    public static SearchResponse from(SearchQuery query, SearchResultPage page, double tookMs) {
        return new SearchResponse(
                query.text(),
                page.page(),
                page.size(),
                page.totalHits(),
                page.totalPages(),
                Math.round(tookMs * 10) / 10.0,
                page.hits().stream().map(SearchHitResponse::from).toList());
    }
}