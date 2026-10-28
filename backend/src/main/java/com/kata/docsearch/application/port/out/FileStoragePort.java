package com.kata.docsearch.application.port.out;

import com.kata.docsearch.domain.model.DocumentFormat;

import java.io.InputStream;
import java.util.UUID;

public interface FileStoragePort {
    /** Saves the file and returns its storage key. */
    String store(UUID documentId, DocumentFormat format, InputStream content);

    InputStream open(String storageKey);

    void delete(String storageKey);
}
