package com.kata.docsearch.application.service;

import com.kata.docsearch.application.port.in.UploadDocumentCommand;
import com.kata.docsearch.application.port.in.UploadDocumentUseCase;
import com.kata.docsearch.application.port.out.ContentTypeDetectorPort;
import com.kata.docsearch.application.port.out.DocumentRepositoryPort;
import com.kata.docsearch.application.port.out.FileStoragePort;
import com.kata.docsearch.application.port.out.ProcessingQueuePort;
import com.kata.docsearch.domain.exception.InvalidDocumentException;
import com.kata.docsearch.domain.exception.UnsupportedDocumentFormatException;
import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.UUID;

/**
 * Validates, stores and enqueues. Without @Transactional on purpose: the document must be
 * confirmed in the database BEFORE publishing the message, or the consumer might not find it.
 */
@Service
@RequiredArgsConstructor
public class UploadDocumentService implements UploadDocumentUseCase {

    private final ContentTypeDetectorPort contentTypeDetector;
    private final FileStoragePort fileStorage;
    private final DocumentRepositoryPort repository;
    private final ProcessingQueuePort processingQueue;
    private final Clock clock;

    @Override
    public Document upload(UploadDocumentCommand command) {
        if (command.size() <= 0) {
            throw new InvalidDocumentException("The file is empty");
        }
        DocumentFormat format = DocumentFormat.fromFilename(command.originalFilename())
                .orElseThrow(() -> new UnsupportedDocumentFormatException(
                        "Only PDF, TXT and Markdown (.md) files are allowed"));

        String contentType = detectContentType(command);
        if (!format.accepts(contentType)) {
            throw new UnsupportedDocumentFormatException(
                    "The file content (" + contentType + ") does not match the ." + format.extension() + " extension");
        }

        UUID id = UUID.randomUUID();
        String storageKey = storeFile(id, format, command);
        var file = new StoredFile(command.originalFilename(), format, contentType, command.size(), storageKey);

        Document document;
        try {
            document = repository.save(Document.newPending(id, command.metadata(), file, clock.instant()));
        } catch (RuntimeException e) {
            fileStorage.delete(storageKey); // do not leave orphan files in case of database failure
            throw e;
        }

        try {
            processingQueue.enqueue(id);
        } catch (RuntimeException e) {
            document.markFailed("The document could not be queued for processing", clock.instant());
            repository.save(document);
            throw e;
        }
        return document;
    }

    private String detectContentType(UploadDocumentCommand command) {
        try (InputStream content = command.content().open()) {
            return contentTypeDetector.detect(content, command.originalFilename());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read uploaded file", e);
        }
    }

    private String storeFile(UUID id, DocumentFormat format, UploadDocumentCommand command) {
        try {
            return fileStorage.store(id, format, command.content().open());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read uploaded file", e);
        }
    }
}