package com.kata.docsearch.infrastructure.adapter.out.storage;

import com.kata.docsearch.application.port.out.FileStoragePort;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.infrastructure.config.AppProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Saves to local disk. To scale, it is replaced by an S3/MinIO adapter with the same port.*/
@Component
class LocalFileStorageAdapter implements FileStoragePort {

    private final Path root;

    LocalFileStorageAdapter(AppProperties properties) {
        this.root = Path.of(properties.storage().path()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage directory " + root, e);
        }
    }

    @Override
    public String store(UUID documentId, DocumentFormat format, InputStream content) {
        String key = documentId + "." + format.extension();
        try (content) {
            Files.copy(content, resolve(key), StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store file " + key, e);
        }
    }

    @Override
    public InputStream open(String storageKey) {
        try {
            return Files.newInputStream(resolve(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot open file " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete file " + storageKey, e);
        }
    }

    /** Defense in depth: no key can escape the root directory. */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return path;
    }
}