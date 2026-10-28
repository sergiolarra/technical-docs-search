package com.kata.docsearch.domain.model;

import java.util.List;

public record SearchResultPage(List<SearchHit> hits, int page, int size, long totalHits) {

    public SearchResultPage {
        hits = List.copyOf(hits);
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalHits / size);
    }
}