package com.kata.docsearch.application.port.out;

import com.kata.docsearch.domain.model.SearchQuery;
import com.kata.docsearch.domain.model.SearchResultPage;

/**
 * Full-text search engine. Today backed by PostgreSQL FTS; an OpenSearch adapter
 * could replace it without touching the use case.
 */
public interface SearchEnginePort {
    SearchResultPage search(SearchQuery query);
}