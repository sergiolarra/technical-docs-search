package com.kata.docsearch.domain.model;

import com.kata.docsearch.domain.exception.InvalidDocumentException;
import com.kata.docsearch.domain.exception.InvalidDocumentStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

    private Document pending() {
        var metadata = new DocumentMetadata("Guía API", "Ana", "Backend", List.of("Java", " java ", "REST"), "1.0");
        var file = new StoredFile("guia.md", DocumentFormat.MARKDOWN, "text/x-web-markdown", 120, "abc.md");
        return Document.newPending(UUID.randomUUID(), metadata, file, NOW);
    }

    @Test
    void newDocumentStartsProcessing() {
        assertThat(pending().getStatus()).isEqualTo(DocumentStatus.PROCESANDO);
    }

    @Test
    void markIndexedStoresContent() {
        var doc = pending();
        doc.markIndexed("contenido", NOW.plusSeconds(1));

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.INDEXADO);
        assertThat(doc.getContent()).isEqualTo("contenido");
    }

    @Test
    void markFailedStoresReason() {
        var doc = pending();
        doc.markFailed("PDF corrupto", NOW.plusSeconds(1));

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.ERROR);
        assertThat(doc.getErrorMessage()).isEqualTo("PDF corrupto");
    }

    @Test
    void cannotChangeStateOnceIndexed() {
        var doc = pending();
        doc.markIndexed("contenido", NOW);

        assertThatThrownBy(() -> doc.markFailed("x", NOW)).isInstanceOf(InvalidDocumentStateException.class);
    }

    @Test
    void tagsAreNormalizedAndDeduplicated() {
        assertThat(pending().getMetadata().tags()).containsExactly("java", "rest");
    }

    @Test
    void blankTitleIsRejected() {
        assertThatThrownBy(() -> new DocumentMetadata(" ", "Ana", "Backend", List.of(), "1.0"))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void formatIsResolvedFromExtension() {
        assertThat(DocumentFormat.fromFilename("Manual.PDF")).contains(DocumentFormat.PDF);
        assertThat(DocumentFormat.fromFilename("script.exe")).isEmpty();
    }
}