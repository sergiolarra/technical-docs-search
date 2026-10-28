package com.kata.docsearch.infrastructure.adapter.out.search;

import com.kata.docsearch.domain.model.HighlightedText.Fragment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeadlineParserTest {

    @Test
    void splitsMarkedTextIntoFragments() {
        var result = HeadlineParser.parse("La \uE000búsqueda\uE001 de \uE000texto\uE001");

        assertThat(result.fragments()).containsExactly(
                new Fragment("La ", false),
                new Fragment("búsqueda", true),
                new Fragment(" de ", false),
                new Fragment("texto", true));
        assertThat(result.plainText()).isEqualTo("La búsqueda de texto");
    }

    @Test
    void textWithoutMatchesIsASinglePlainFragment() {
        assertThat(HeadlineParser.parse("Manual de despliegue").fragments())
                .containsExactly(new Fragment("Manual de despliegue", false));
    }

    @Test
    void nullProducesNoFragments() {
        assertThat(HeadlineParser.parse(null).fragments()).isEmpty();
    }
}