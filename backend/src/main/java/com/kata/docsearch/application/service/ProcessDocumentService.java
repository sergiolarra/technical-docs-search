package com.kata.docsearch.application.service;

import com.kata.docsearch.application.port.in.ProcessDocumentUseCase;
import com.kata.docsearch.application.port.out.DocumentRepositoryPort;
import com.kata.docsearch.application.port.out.FileStoragePort;
import com.kata.docsearch.application.port.out.StatusNotifierPort;
import com.kata.docsearch.application.port.out.TextExtractorPort;
import com.kata.docsearch.domain.exception.DocumentExtractionException;
import com.kata.docsearch.domain.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDocumentService implements ProcessDocumentUseCase {

    private static final int MAX_ERROR_LENGTH = 500;

    private final DocumentRepositoryPort repository;
    private final FileStoragePort fileStorage;
    private final TextExtractorPort textExtractor;
    private final StatusNotifierPort statusNotifier;
    private final Clock clock;

    /**
     * Idempotent: the row is locked during processing and only acts if it is still PROCESSING.
     * A duplicate message waits for the first and then does nothing.
     */
    @Override
    @Transactional
    public void process(UUID documentId) {
        var found = repository.findByIdForUpdate(documentId);
        if (found.isEmpty()) {
            log.warn("Document {} not found; message discarded", documentId);
            return;
        }
        Document document = found.get();
        if (!document.isProcessing()) {
            log.info("Document {} is already {}; skipping", documentId, document.getStatus());
            return;
        }

        try (InputStream content = fileStorage.open(document.getFile().storageKey())) {
            String text = textExtractor.extract(content, document.getFile().format());
            if (text.isBlank()) {
                document.markFailed("No text could be extracted (empty or scanned document)", clock.instant());
            } else {
                document.markIndexed(text, clock.instant());
            }
        } catch (DocumentExtractionException e) {
            document.markFailed(truncate(e.getMessage()), clock.instant());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        repository.save(document);
        statusNotifier.documentStatusChanged(document);
    }

    @Override
    @Transactional
    public void markAsFailed(UUID documentId, String reason) {
        repository.findByIdForUpdate(documentId)
                .filter(Document::isProcessing)
                .ifPresent(document -> {
                    document.markFailed(truncate(reason), clock.instant());
                    repository.save(document);
                    statusNotifier.documentStatusChanged(document);
                });
    }

    private static String truncate(String message) {
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH - 3) + "...";
    }
}