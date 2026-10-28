package com.kata.docsearch.application.service;

import com.kata.docsearch.application.port.in.SearchDocumentsUseCase;
import com.kata.docsearch.application.port.out.SearchEnginePort;
import com.kata.docsearch.domain.model.SearchQuery;
import com.kata.docsearch.domain.model.SearchResultPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchDocumentsService implements SearchDocumentsUseCase {

    /** Half of the 1 s budget: slower searches are logged so they can be investigated. */
    private static final Duration SLOW_SEARCH_THRESHOLD = Duration.ofMillis(500);

    private final SearchEnginePort searchEngine;

    @Override
    public SearchResultPage search(SearchQuery query) {
        long start = System.nanoTime();
        SearchResultPage result = searchEngine.search(query);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        if (elapsed.compareTo(SLOW_SEARCH_THRESHOLD) > 0) {
            log.warn("Slow search: {} ms for '{}' (page {}, {} hits)",
                    elapsed.toMillis(), query.text(), query.page(), result.totalHits());
        }
        return result;
    }
}