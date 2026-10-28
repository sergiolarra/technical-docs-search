package com.kata.docsearch.application.port.out;

import java.util.UUID;

public interface ProcessingQueuePort {
    void enqueue(UUID documentId);
}
