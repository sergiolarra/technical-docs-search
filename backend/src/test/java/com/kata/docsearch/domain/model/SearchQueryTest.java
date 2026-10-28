package com.kata.docsearch.domain.model;

import com.kata.docsearch.domain.exception.InvalidSearchQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchQueryTest {

    @Test
    void normalizesTextCategoryAndTags() {
        var query = new SearchQuery("  arquitectura  ", "  ", List.of("Java", "java", " REST "), 0, 10);

        assertThat(query.text()).isEqualTo("arquitectura");
        assertThat(query.category()).isNull();
        assertThat(query.tags()).containsExactly("java", "rest");
    }

    @Test
    void computesOffsetFromPageAndSize() {
        assertThat(new SearchQuery("x", null, null, 3, 20).offset()).isEqualTo(60);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankTextIsRejected(String text) {
        assertThatThrownBy(() -> new SearchQuery(text, null, null, 0, 10))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    void tooLongTextIsRejected() {
        assertThatThrownBy(() -> new SearchQuery("a".repeat(201), null, null, 0, 10))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    void pageSizeOutOfRangeIsRejected(int size) {
        assertThatThrownBy(() -> new SearchQuery("x", null, null, 0, size))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    void negativePageIsRejected() {
        assertThatThrownBy(() -> new SearchQuery("x", null, null, -1, 10))
                .isInstanceOf(InvalidSearchQueryException.class);
    }
}