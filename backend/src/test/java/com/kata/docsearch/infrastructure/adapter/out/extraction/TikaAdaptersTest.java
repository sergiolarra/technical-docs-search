package com.kata.docsearch.infrastructure.adapter.out.extraction;

import com.kata.docsearch.domain.exception.DocumentExtractionException;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.infrastructure.config.AppProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TikaAdaptersTest {

    private final TikaContentTypeDetector detector = new TikaContentTypeDetector();
    private final TikaTextExtractor extractor = new TikaTextExtractor(new AppProperties(
            new AppProperties.Storage("./uploads"),
            new AppProperties.Processing(1_000_000),
            new AppProperties.Events(Duration.ofMinutes(30), Duration.ofSeconds(20))));

    @Test
    void detectsPdfByContentEvenWithWrongExtension() throws IOException {
        String type = detector.detect(new ByteArrayInputStream(samplePdf("hola")), "disguised.txt");
        assertThat(type).isEqualTo("application/pdf");
    }

    @Test
    void detectsMarkdownByName() {
        String type = detector.detect(stream("# Título"), "guia.md");
        assertThat(DocumentFormat.MARKDOWN.accepts(type)).isTrue();
    }

    @Test
    void extractsTextFromPdf() throws IOException {
        String text = extractor.extract(new ByteArrayInputStream(samplePdf("Arquitectura hexagonal")), DocumentFormat.PDF);
        assertThat(text).contains("Arquitectura hexagonal");
    }

    @Test
    void extractsUtf8TextWithAccents() {
        String text = extractor.extract(stream("Configuración de búsqueda"), DocumentFormat.TXT);
        assertThat(text).isEqualTo("Configuración de búsqueda");
    }

    @Test
    void corruptPdfRaisesExtractionException() {
        assertThatThrownBy(() -> extractor.extract(stream("esto no es un pdf"), DocumentFormat.PDF))
                .isInstanceOf(DocumentExtractionException.class);
    }

    private static ByteArrayInputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] samplePdf(String text) throws IOException {
        try (var document = new PDDocument()) {
            var page = new PDPage();
            document.addPage(page);
            try (var content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            var out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}