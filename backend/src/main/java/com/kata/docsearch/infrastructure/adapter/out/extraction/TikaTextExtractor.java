package com.kata.docsearch.infrastructure.adapter.out.extraction;

import com.kata.docsearch.application.port.out.TextExtractorPort;
import com.kata.docsearch.domain.exception.DocumentExtractionException;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.infrastructure.config.AppProperties;
import org.apache.commons.io.input.TaggedInputStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;


import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Component
class TikaTextExtractor implements TextExtractorPort {

    private final int maxChars;

    TikaTextExtractor(AppProperties properties) {
        this.maxChars = properties.processing().maxExtractedChars();
    }

    @Override
    public String extract(InputStream content, DocumentFormat format) {
        Parser parser = switch (format) {
            case PDF -> new PDFParser();
            case TXT, MARKDOWN -> new TXTParser(); // detecta la codificación (UTF-8, ISO-8859-1...)
        };
        var handler = new BodyContentHandler(maxChars);
        // Marca las excepciones del stream original para distinguir "no pude leer" de "no pude interpretar"
        TaggedInputStream tagged = new TaggedInputStream(content);
        try (TikaInputStream stream = TikaInputStream.get(tagged)) {
            parser.parse(stream, handler, new Metadata(), new ParseContext());
            return handler.toString().strip();
        } catch (IOException e) {
            if (tagged.isCauseOf(e)) {
                // Fallo al leer el archivo almacenado (disco): se propaga para que la cola reintente
                throw new UncheckedIOException("Cannot read stored file", e);
            }
            // El parser no entiende el contenido (p. ej. PDF corrupto): reintentar no lo arregla
            throw new DocumentExtractionException("Text could not be extracted: " + e.getMessage());
        } catch (SAXException | TikaException e) {
            // Contenido inválido o por encima del límite de caracteres
            throw new DocumentExtractionException("Text could not be extracted: " + e.getMessage());
        }
    }
}