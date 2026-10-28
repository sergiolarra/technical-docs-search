package com.kata.docsearch.infrastructure.adapter.in.web.dto;

import com.kata.docsearch.domain.model.HighlightedText;

import java.util.List;

/** A piece of highlighted text; clients wrap fragments with match=true in a <mark> element. */
public record FragmentResponse(String text, boolean match) {

    public static List<FragmentResponse> from(HighlightedText highlighted) {
        return highlighted.fragments().stream()
                .map(fragment -> new FragmentResponse(fragment.text(), fragment.match()))
                .toList();
    }
}