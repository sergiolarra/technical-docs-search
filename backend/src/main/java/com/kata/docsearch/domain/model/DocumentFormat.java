package com.kata.docsearch.domain.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum DocumentFormat {
    PDF("pdf", Set.of("application/pdf")),
    TXT("txt", Set.of("text/plain")),
    MARKDOWN("md", Set.of("text/markdown", "text/x-web-markdown","text/plain"));

    private final String extension;
    private final Set<String> acceptedContentTypes;

    DocumentFormat(String extension, Set<String> acceptedContentTypes) {
        this.extension = extension;
        this.acceptedContentTypes = acceptedContentTypes;
    }

    public static Optional<DocumentFormat> fromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return Optional.empty();
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(f -> f.extension.equals(ext)).findFirst();
    }

    public boolean accepts(String detectedContentType) {
        return acceptedContentTypes.contains(detectedContentType);
    }

    public String extension() {
        return extension;
    }

}
