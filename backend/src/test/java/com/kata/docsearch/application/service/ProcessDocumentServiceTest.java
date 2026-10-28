package com.kata.docsearch.application.service;

import com.kata.docsearch.application.port.out.DocumentRepositoryPort;
import com.kata.docsearch.application.port.out.FileStoragePort;
import com.kata.docsearch.application.port.out.StatusNotifierPort;
import com.kata.docsearch.application.port.out.TextExtractorPort;
import com.kata.docsearch.domain.exception.DocumentExtractionException;
import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.DocumentMetadata;
import com.kata.docsearch.domain.model.DocumentStatus;
import com.kata.docsearch.domain.model.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessDocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

    @Mock
    private DocumentRepositoryPort repository;
    @Mock
    private FileStoragePort fileStorage;
    @Mock
    private TextExtractorPort textExtractor;
    @Mock
    private StatusNotifierPort statusNotifier;

    private ProcessDocumentService service;

    @BeforeEach
    void setUp() {
        service = new ProcessDocumentService(repository, fileStorage, textExtractor, statusNotifier,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void documentIsIndexedWithExtractedText() {
        Document document = givenStoredDocument();
        when(textExtractor.extract(any(), eq(DocumentFormat.MARKDOWN))).thenReturn("Arquitectura hexagonal");

        service.process(document.getId());

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.INDEXADO);
        assertThat(document.getContent()).isEqualTo("Arquitectura hexagonal");
        verify(repository).save(document);
        verify(statusNotifier).documentStatusChanged(document);
    }

    @Test
    void extractionErrorMarksDocumentAsFailed() {
        Document document = givenStoredDocument();
        when(textExtractor.extract(any(), any())).thenThrow(new DocumentExtractionException("corrupt file"));

        service.process(document.getId());

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.ERROR);
        assertThat(document.getErrorMessage()).isEqualTo("corrupt file");
        verify(statusNotifier).documentStatusChanged(document);
    }

    @Test
    void documentWithoutTextIsMarkedAsFailed() {
        Document document = givenStoredDocument();
        when(textExtractor.extract(any(), any())).thenReturn("   ");

        service.process(document.getId());

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.ERROR);
    }

    @Test
    void duplicateMessageForIndexedDocumentIsIgnored() {
        Document document = pendingDocument();
        document.markIndexed("already processed", NOW);
        when(repository.findByIdForUpdate(document.getId())).thenReturn(Optional.of(document));

        service.process(document.getId());

        verifyNoInteractions(fileStorage, textExtractor, statusNotifier);
        verify(repository, never()).save(any());
    }

    @Test
    void unknownDocumentIsIgnored() {
        when(repository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        service.process(UUID.randomUUID());

        verifyNoInteractions(fileStorage, textExtractor, statusNotifier);
    }

    @Test
    void infrastructureErrorsPropagateSoTheQueueRetries() {
        Document document = pendingDocument();
        when(repository.findByIdForUpdate(document.getId())).thenReturn(Optional.of(document));
        when(fileStorage.open("key.md")).thenThrow(new UncheckedIOException(new IOException("disk unavailable")));

        assertThatThrownBy(() -> service.process(document.getId())).isInstanceOf(UncheckedIOException.class);
        assertThat(document.isProcessing()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void markAsFailedOnlyAffectsProcessingDocuments() {
        Document indexed = pendingDocument();
        indexed.markIndexed("text", NOW);
        when(repository.findByIdForUpdate(indexed.getId())).thenReturn(Optional.of(indexed));

        service.markAsFailed(indexed.getId(), "retries exhausted");

        assertThat(indexed.getStatus()).isEqualTo(DocumentStatus.INDEXADO);
        verify(repository, never()).save(any());
    }

    private Document givenStoredDocument() {
        Document document = pendingDocument();
        when(repository.findByIdForUpdate(document.getId())).thenReturn(Optional.of(document));
        when(fileStorage.open("key.md")).thenReturn(new ByteArrayInputStream(new byte[0]));
        return document;
    }

    private static Document pendingDocument() {
        var metadata = new DocumentMetadata("Guía", "Ana", "Backend", List.of(), "1.0");
        var file = new StoredFile("guia.md", DocumentFormat.MARKDOWN, "text/x-web-markdown", 6, "key.md");
        return Document.newPending(UUID.randomUUID(), metadata, file, NOW);
    }
}