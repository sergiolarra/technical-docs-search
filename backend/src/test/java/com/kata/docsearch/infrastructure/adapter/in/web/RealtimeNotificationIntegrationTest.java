package com.kata.docsearch.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.kata.docsearch.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A real HTTP server and a real SSE client: upload -> queue -> consumer -> fanout -> SSE -> browser. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.storage.path=${java.io.tmpdir}/docsearch-test-uploads")
@Import(TestcontainersConfiguration.class)
class RealtimeNotificationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Environment environment;

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final List<Stream<String>> openStreams = new ArrayList<>();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void closeStreams() {
        openStreams.forEach(Stream::close);
    }

    @Test
    void indexedDocumentIsPushedToEveryOpenTab() throws Exception {
        BlockingQueue<String> firstTab = subscribe();
        BlockingQueue<String> secondTab = subscribe();

        String id = upload("eventos.md", "# Eventos\n\nNotificaciones en tiempo real con SSE");

        awaitEvent(firstTab, id, "INDEXADO");
        awaitEvent(secondTab, id, "INDEXADO");
    }

    @Test
    void failedDocumentIsPushedAsError() throws Exception {
        BlockingQueue<String> tab = subscribe();

        String id = upload("roto.pdf", "%PDF-1.7 this is not a valid pdf body");

        awaitEvent(tab, id, "ERROR");
    }

    /** Opens the stream like a browser EventSource and waits for the initial "connected" event. */
    private BlockingQueue<String> subscribe() throws Exception {
        String url = "http://localhost:" + environment.getProperty("local.server.port") + "/api/events";
        var request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<Stream<String>> response = http.send(request, HttpResponse.BodyHandlers.ofLines());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("text/event-stream"));

        BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        Stream<String> body = response.body();
        openStreams.add(body);
        Thread.ofVirtual().start(() -> {
            try {
                body.forEach(lines::add);
            } catch (RuntimeException ignored) {
                // stream closed by the test
            }
        });
        await().atMost(Duration.ofSeconds(5)).until(() -> lines.contains("event:connected"));
        return lines;
    }

    private String upload(String filename, String content) throws Exception {
        var file = new MockMultipartFile("file", filename, "application/octet-stream",
                content.getBytes(StandardCharsets.UTF_8));
        String body = mvc.perform(multipart("/api/documents").file(file)
                        .param("title", "Prueba tiempo real")
                        .param("author", "QA")
                        .param("category", "Pruebas")
                        .param("version", "1.0"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.id");
    }

    private static void awaitEvent(BlockingQueue<String> lines, String documentId, String expectedStatus) {
        await().atMost(Duration.ofSeconds(15)).until(() -> lines.stream().anyMatch(line ->
                line.startsWith("data:") && line.contains(documentId)
                        && line.contains("\"status\":\"" + expectedStatus + "\"")));
    }
}