package com.kata.docsearch.domain.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Text split into plain and matching fragments. The API returns structure instead of HTML,
 * so clients render highlights safely (no markup from the server, no XSS).
 */
public record HighlightedText(List<Fragment> fragments) {

    public record Fragment(String text, boolean match) {
    }

    public HighlightedText {
        fragments = List.copyOf(fragments);
    }

    public String plainText() {
        return fragments.stream().map(Fragment::text).collect(Collectors.joining());
    }
}