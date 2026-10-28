package com.kata.docsearch.application.port.in;

import com.kata.docsearch.domain.model.SearchQuery;
import com.kata.docsearch.domain.model.SearchResultPage;

public interface SearchDocumentsUseCase {
    SearchResultPage search(SearchQuery query);
}