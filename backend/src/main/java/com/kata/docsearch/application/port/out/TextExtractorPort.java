package com.kata.docsearch.application.port.out;

import com.kata.docsearch.domain.model.DocumentFormat;

import java.io.InputStream;

public interface TextExtractorPort {

    /** @throws com.kata.docsearch.domain.exception.DocumentExtractionException if the file cannot be read*/
    String extract(InputStream content, DocumentFormat format);
}
