package com.kata.docsearch.infrastructure.adapter.out.search;

import com.kata.docsearch.domain.model.HighlightedText;
import com.kata.docsearch.domain.model.HighlightedText.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns ts_headline output into structured fragments. The markers are Unicode private-use
 * characters, which never appear in real text, so they cannot collide with document content.
 */
final class HeadlineParser {

    static final char MATCH_START = '\uE000';
    static final char MATCH_END = '\uE001';

    private HeadlineParser() {
    }

    static HighlightedText parse(String marked) {
        List<Fragment> fragments = new ArrayList<>();
        if (marked == null) {
            return new HighlightedText(fragments);
        }
        StringBuilder current = new StringBuilder();
        boolean inMatch = false;
        for (char c : marked.toCharArray()) {
            if (c == MATCH_START || c == MATCH_END) {
                if (!current.isEmpty()) {
                    fragments.add(new Fragment(current.toString(), inMatch));
                    current.setLength(0);
                }
                inMatch = c == MATCH_START;
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            fragments.add(new Fragment(current.toString(), inMatch));
        }
        return new HighlightedText(fragments);
    }
}