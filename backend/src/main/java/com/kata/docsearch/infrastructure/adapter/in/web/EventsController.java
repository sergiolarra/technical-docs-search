package com.kata.docsearch.infrastructure.adapter.in.web;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
class EventsController {

    private final SseEmitterRegistry sseEmitters;

    /** HU-04: a single stream per browser tab with every document status change. No polling. */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribe(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no"); // tells nginx not to buffer the stream
        return sseEmitters.register();
    }
}