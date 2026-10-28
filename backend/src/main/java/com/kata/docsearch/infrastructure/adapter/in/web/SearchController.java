package com.kata.docsearch.infrastructure.adapter.in.web;

import com.kata.docsearch.application.port.in.SearchDocumentsUseCase;
import com.kata.docsearch.domain.model.SearchQuery;
import com.kata.docsearch.domain.model.SearchResultPage;
import com.kata.docsearch.infrastructure.adapter.in.web.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
class SearchController {

    private final SearchDocumentsUseCase searchDocuments;

    /**
     * HU-02: full-text search over title, metadata and content.
     * Supports web search syntax: "exact phrase", -excluded, or. The search time is exposed in the
     * Server-Timing header so it can be checked in the browser DevTools.
     */
    @GetMapping("/search")
    ResponseEntity<SearchResponse> search(@RequestParam("q") String text,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) List<String> tags,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        var query = new SearchQuery(text, category, tags, page, size);

        long start = System.nanoTime();
        SearchResultPage result = searchDocuments.search(query);
        double tookMs = (System.nanoTime() - start) / 1_000_000.0;

        return ResponseEntity.ok()
                .header("Server-Timing", String.format(Locale.ROOT, "search;desc=\"Full-text search\";dur=%.1f", tookMs))
                .body(SearchResponse.from(query, result, tookMs));
    }
}