package com.kata.docsearch.domain.model;

import com.kata.docsearch.domain.exception.InvalidSearchQueryException;

import java.util.List;

/**
 * A validated search request. Supports web search syntax: "exact phrase", -excluded, or.
 */
public record SearchQuery(String text, String category, List<String> tags, int page, int size) {

    public static final int MAX_TEXT_LENGTH = 200;
    public static final int MAX_PAGE_SIZE = 50;

    public SearchQuery {
        if (text == null || text.isBlank()) {
            throw new InvalidSearchQueryException("The search text is required");
        }
        text = text.strip();
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new InvalidSearchQueryException("The search text cannot exceed " + MAX_TEXT_LENGTH + " characters");
        }
        if (page < 0) {
            throw new InvalidSearchQueryException("The page number cannot be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidSearchQueryException("The page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        category = category == null || category.isBlank() ? null : category.strip();
        tags = Tags.normalize(tags);
    }

    public int offset() {
        return page * size;
    }
}