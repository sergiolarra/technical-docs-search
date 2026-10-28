package com.kata.docsearch.infrastructure.adapter.in.web;

import com.kata.docsearch.TestcontainersConfiguration;
import com.kata.docsearch.application.port.out.DocumentRepositoryPort;
import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.DocumentMetadata;
import com.kata.docsearch.domain.model.StoredFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real SQL against PostgreSQL. Uses vocabulary that no other test uses, so results
 * are deterministic even though the database is shared with other integration tests.
 * Same @SpringBootTest configuration as DocumentUploadIntegrationTest, so the context is reused.
 */
@SpringBootTest(properties = "app.storage.path=${java.io.tmpdir}/docsearch-test-uploads")
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DocumentRepositoryPort repository;

    private MockMvc mvc;

    @BeforeAll
    void seed() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        indexed("Guía de observabilidad", "Operaciones", List.of("metricas", "monitoreo"),
                "Métricas, trazas y registros para la observabilidad de microservicios.", NOW);
        indexed("Manual de despliegue en Kubernetes", "Operaciones", List.of("kubernetes"),
                "Pasos para desplegar el clúster. Al final se menciona la observabilidad una sola vez.", NOW.minusSeconds(60));
        indexed("Trazabilidad de pedidos", "Negocio", List.of("pedidos"),
                "La telemetría permite seguir cada pedido de principio a fin.", NOW.minusSeconds(120));
        pending("Observabilidad pendiente de procesar");
        for (int i = 1; i <= 12; i++) {
            indexed("Informe de latencia " + i, "Rendimiento", List.of("informes"),
                    "Análisis de latencia por percentil.", NOW.minusSeconds(1000 + i));
        }
    }

    @Test
    void searchIsAccentAndCaseInsensitiveAndRanksTitleMatchesFirst() throws Exception {
        mvc.perform(get("/api/documents/search").param("q", "OBSERVABILIDAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(2))
                .andExpect(jsonPath("$.results[*].title",
                        contains("Guía de observabilidad", "Manual de despliegue en Kubernetes")))
                .andExpect(jsonPath("$.results[0].titleHighlight[?(@.match == true)].text", hasItem("observabilidad")));
    }

    @Test
    void highlightKeepsTheOriginalAccentedWord() throws Exception {
        mvc.perform(get("/api/documents/search").param("q", "telemetria"))
                .andExpect(jsonPath("$.results[0].title").value("Trazabilidad de pedidos"))
                .andExpect(jsonPath("$.results[0].snippet[?(@.match == true)].text", hasItem("telemetría")));
    }

    @Test
    void exactPhraseOnlyMatchesConsecutiveWords() throws Exception {
        mvc.perform(get("/api/documents/search").param("q", "\"trazas y registros\""))
                .andExpect(jsonPath("$.results[*].title", contains("Guía de observabilidad")));
    }

    @Test
    void excludedTermsRemoveResults() throws Exception {
        mvc.perform(get("/api/documents/search").param("q", "observabilidad -kubernetes"))
                .andExpect(jsonPath("$.results[*].title", contains("Guía de observabilidad")));
    }

    @Test
    void categoryAndTagFiltersNarrowResults() throws Exception {
        mvc.perform(get("/api/documents/search")
                        .param("q", "observabilidad")
                        .param("category", "operaciones")
                        .param("tags", "Kubernetes"))
                .andExpect(jsonPath("$.results[*].title", contains("Manual de despliegue en Kubernetes")));
    }

    @Test
    void paginatesWithTotals() throws Exception {
        mvc.perform(get("/api/documents/search").param("q", "latencia").param("size", "5"))
                .andExpect(jsonPath("$.results", hasSize(5)))
                .andExpect(jsonPath("$.totalHits").value(12))
                .andExpect(jsonPath("$.totalPages").value(3));

        mvc.perform(get("/api/documents/search").param("q", "latencia").param("size", "5").param("page", "2"))
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.totalHits").value(12));

        mvc.perform(get("/api/documents/search").param("q", "latencia").param("size", "5").param("page", "3"))
                .andExpect(jsonPath("$.results", hasSize(0)))
                .andExpect(jsonPath("$.totalHits").value(12));
    }

    @Test
    void exposesSearchTimeInServerTimingHeader() throws Exception {
        mvc.perform(get("/api/documents/search").param("q", "latencia"))
                .andExpect(header().string("Server-Timing", containsString("search;")))
                .andExpect(jsonPath("$.tookMs").isNumber());
    }

    @Test
    void invalidQueriesReturn400() throws Exception {
        mvc.perform(get("/api/documents/search").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid search query"));
        mvc.perform(get("/api/documents/search").param("q", "latencia").param("size", "100"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/documents/search"))
                .andExpect(status().isBadRequest());
    }

    private void indexed(String title, String category, List<String> tags, String content, Instant createdAt) {
        Document document = Document.newPending(UUID.randomUUID(), metadata(title, category, tags), file(), createdAt);
        document.markIndexed(content, createdAt);
        repository.save(document);
    }

    private void pending(String title) {
        repository.save(Document.newPending(UUID.randomUUID(), metadata(title, "Operaciones", List.of()), file(), NOW));
    }

    private static DocumentMetadata metadata(String title, String category, List<String> tags) {
        return new DocumentMetadata(title, "Equipo QA", category, tags, "1.0");
    }

    private static StoredFile file() {
        return new StoredFile("seed.txt", DocumentFormat.TXT, "text/plain", 1, "seed.txt");
    }
}