package com.kata.docsearch.application.service;

import com.kata.docsearch.application.port.in.UploadDocumentCommand;
import com.kata.docsearch.application.port.out.ContentTypeDetectorPort;
import com.kata.docsearch.application.port.out.DocumentRepositoryPort;
import com.kata.docsearch.application.port.out.FileStoragePort;
import com.kata.docsearch.application.port.out.ProcessingQueuePort;
import com.kata.docsearch.domain.exception.InvalidDocumentException;
import com.kata.docsearch.domain.exception.UnsupportedDocumentFormatException;
import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.DocumentMetadata;
import com.kata.docsearch.domain.model.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadDocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

    @Mock
    private ContentTypeDetectorPort contentTypeDetector;
    @Mock
    private FileStoragePort fileStorage;
    @Mock
    private DocumentRepositoryPort repository;
    @Mock
    private ProcessingQueuePort processingQueue;

    private UploadDocumentService service;

    @BeforeEach
    void setUp() {
        service = new UploadDocumentService(contentTypeDetector, fileStorage, repository, processingQueue,
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void validDocumentIsSavedBeforeBeingQueued() {
        givenValidMarkdownIsStored();

        Document document = service.upload(command("guia.md", 6));

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESANDO);
        assertThat(document.getFile().storageKey()).isEqualTo("key.md");
        InOrder order = inOrder(repository, processingQueue);
        order.verify(repository).save(any());
        order.verify(processingQueue).enqueue(document.getId());
    }

    @Test
    void emptyFileIsRejected() {
        assertThatThrownBy(() -> service.upload(command("guia.md", 0)))
                .isInstanceOf(InvalidDocumentException.class);
        verifyNoInteractions(fileStorage, processingQueue);
    }

    @Test
    void unsupportedExtensionIsRejected() {
        assertThatThrownBy(() -> service.upload(command("setup.exe", 6)))
                .isInstanceOf(UnsupportedDocumentFormatException.class);
        verifyNoInteractions(contentTypeDetector, fileStorage);
    }

    @Test
    void contentNotMatchingExtensionIsRejected() {
        when(contentTypeDetector.detect(any(), eq("manual.pdf"))).thenReturn("application/x-msdownload");

        assertThatThrownBy(() -> service.upload(command("manual.pdf", 6)))
                .isInstanceOf(UnsupportedDocumentFormatException.class);
        verifyNoInteractions(fileStorage, processingQueue);
    }

    @Test
    void storedFileIsDeletedWhenSaveFails() {
        givenValidMarkdownIsStored();
        when(repository.save(any())).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> service.upload(command("guia.md", 6))).isInstanceOf(IllegalStateException.class);
        verify(fileStorage).delete("key.md");
        verifyNoInteractions(processingQueue);
    }

    @Test
    void documentIsMarkedAsFailedWhenQueueIsUnavailable() {
        givenValidMarkdownIsStored();
        doThrow(new IllegalStateException("broker down")).when(processingQueue).enqueue(any());

        assertThatThrownBy(() -> service.upload(command("guia.md", 6))).isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<Document> saved = ArgumentCaptor.forClass(Document.class);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DocumentStatus.ERROR);
    }

    private void givenValidMarkdownIsStored() {
        when(contentTypeDetector.detect(any(), eq("guia.md"))).thenReturn("text/x-web-markdown");
        when(fileStorage.store(any(), eq(DocumentFormat.MARKDOWN), any())).thenReturn("key.md");
    }

    private static UploadDocumentCommand command(String filename, long size) {
        var metadata = new DocumentMetadata("Guía", "Ana", "Backend", List.of("java"), "1.0");
        return new UploadDocumentCommand(metadata, filename, size,
                () -> new ByteArrayInputStream("# Guía".getBytes(StandardCharsets.UTF_8)));
    }
}