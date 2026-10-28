package com.kata.docsearch.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.kata.docsearch.TestcontainersConfiguration;
import com.kata.docsearch.application.port.out.DocumentRepositoryPort;
import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end flow against real PostgreSQL and RabbitMQ containers: HTTP -> queue -> consumer -> database. */
@SpringBootTest(properties = "app.storage.path=${java.io.tmpdir}/docsearch-test-uploads")
@Import(TestcontainersConfiguration.class)
class DocumentUploadIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DocumentRepositoryPort repository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void uploadedMarkdownIsIndexedAsynchronously() throws Exception {
        var file = file("file", "guia.md", "# Guía\n\nArquitectura hexagonal con Spring Boot");

        String body = mvc.perform(withMetadata(multipart("/api/documents").file(file)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESANDO"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID id = UUID.fromString(JsonPath.read(body, "$.id"));

        awaitStatus(id, DocumentStatus.INDEXADO);
        mvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(containsString("Arquitectura hexagonal")))
                .andExpect(jsonPath("$.tags[0]").value("java"));
    }

    @Test
    void corruptPdfEndsInError() throws Exception {
        // Starts with the PDF signature, so it passes validation, but the parser cannot read it
        var file = file("file", "roto.pdf", "%PDF-1.7 this is not a valid pdf body");

        String body = mvc.perform(withMetadata(multipart("/api/documents").file(file)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        awaitStatus(UUID.fromString(JsonPath.read(body, "$.id")), DocumentStatus.ERROR);
    }

    @Test
    void disguisedFileIsRejectedWith415() throws Exception {
        var file = file("file", "virus.pdf", "MZ fake executable");

        mvc.perform(withMetadata(multipart("/api/documents").file(file)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Unsupported document format"));
    }

    @Test
    void missingTitleReturnsFieldErrors() throws Exception {
        var file = file("file", "manual.txt", "Manual de despliegue");

        mvc.perform(multipart("/api/documents").file(file)
                        .param("author", "Ana").param("category", "Backend").param("version", "1.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void batchReportsResultPerFile() throws Exception {
        mvc.perform(multipart("/api/documents/batch")
                        .file(file("files", "guia.md", "# Guía"))
                        .file(file("files", "virus.pdf", "MZ fake executable"))
                        .param("author", "Ana").param("category", "Backend").param("version", "1.0"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$[0].accepted").value(true))
                .andExpect(jsonPath("$[0].status").value("PROCESANDO"))
                .andExpect(jsonPath("$[1].accepted").value(false));
    }

    @Test
    void unknownDocumentReturns404() throws Exception {
        mvc.perform(get("/api/documents/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Document not found"));
    }

    private void awaitStatus(UUID id, DocumentStatus expected) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(repository.findById(id)).get().extracting(Document::getStatus).isEqualTo(expected));
    }

    private static MockMultipartHttpServletRequestBuilder withMetadata(MockMultipartHttpServletRequestBuilder request) {
        return (MockMultipartHttpServletRequestBuilder) request
                .param("title", "Guía de prueba")
                .param("author", "Ana Pérez")
                .param("category", "Backend")
                .param("tags", "java", "rest")
                .param("version", "1.0");
    }

    private static MockMultipartFile file(String part, String filename, String content) {
        return new MockMultipartFile(part, filename, "application/octet-stream", content.getBytes(StandardCharsets.UTF_8));
    }
}