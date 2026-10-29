package com.kata.docsearch.infrastructure.adapter.in.web;

import com.kata.docsearch.domain.event.DocumentStatusChanged;
import com.kata.docsearch.infrastructure.config.AppProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Open SSE connections of this instance. Each connection is cheap: it waits on a virtual thread. */
@Slf4j
@Component
public class SseEmitterRegistry {

    /** Tells EventSource how long to wait before reconnecting after the stream closes. */
    private static final long RECONNECT_DELAY_MILLIS = 3_000;

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final long timeoutMillis;

    SseEmitterRegistry(AppProperties properties) {
        this.timeoutMillis = properties.events().emitterTimeout().toMillis();
    }

    public SseEmitter register() {
        var emitter = new SseEmitter(timeoutMillis);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        emitters.add(emitter);
        send(emitter, SseEmitter.event()
                .name("connected")
                .reconnectTime(RECONNECT_DELAY_MILLIS)
                .data(Map.of("connectedAt", Instant.now().toString()), MediaType.APPLICATION_JSON));
        return emitter;
    }

    public void broadcast(DocumentStatusChanged event) {
        // A builder renders its text only once, so every emitter gets its own
        emitters.forEach(emitter -> send(emitter, SseEmitter.event()
                .id(event.documentId() + ":" + event.status())
                .name("document-status")
                .data(event, MediaType.APPLICATION_JSON)));
        log.debug("Document {} {} sent to {} SSE clients", event.documentId(), event.status(), emitters.size());
    }

    /** Keeps idle streams open through proxies and load balancers that close silent connections. */
    @Scheduled(fixedRateString = "${app.events.heartbeat-interval}")
    void heartbeat() {
        emitters.forEach(emitter -> send(emitter, SseEmitter.event().comment("heartbeat")));
    }

    /** On shutdown, close every stream so browsers reconnect to another instance. */
    @PreDestroy
    void closeAll() {
        emitters.forEach(SseEmitter::complete);
        emitters.clear();
    }

    public int activeConnections() {
        return emitters.size();
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // The client went away: stop sending to it; the container completes the request
            emitters.remove(emitter);
        }
    }
}