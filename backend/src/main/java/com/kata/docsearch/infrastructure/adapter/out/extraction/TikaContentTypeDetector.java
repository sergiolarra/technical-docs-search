package com.kata.docsearch.infrastructure.adapter.out.extraction;

import com.kata.docsearch.application.port.out.ContentTypeDetectorPort;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Component
class TikaContentTypeDetector implements ContentTypeDetectorPort {

    private final Tika tika = new Tika();

    @Override
    public String detect(InputStream content, String filename) {
        try (TikaInputStream stream = TikaInputStream.get(content)) {
            return tika.detect(stream, filename);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot detect content type", e);
        }
    }
}