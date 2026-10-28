package com.kata.docsearch.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PROCESSING_QUEUE = "documents.processing";
    public static final String PROCESSING_DLX = "documents.processing.dlx";
    public static final String PROCESSING_DLQ = "documents.processing.dlq";

    /** Durable queue: jobs survive restarts. If retries are exhausted, the message goes to the DLQ. */
    @Bean
    Queue processingQueue() {
        return QueueBuilder.durable(PROCESSING_QUEUE)
                .deadLetterExchange(PROCESSING_DLX)
                .deadLetterRoutingKey(PROCESSING_DLQ)
                .build();
    }

    @Bean
    DirectExchange processingDeadLetterExchange() {
        return new DirectExchange(PROCESSING_DLX, true, false);
    }

    @Bean
    Queue processingDeadLetterQueue() {
        return QueueBuilder.durable(PROCESSING_DLQ).build();
    }

    @Bean
    Binding processingDeadLetterBinding(Queue processingDeadLetterQueue, DirectExchange processingDeadLetterExchange) {
        return BindingBuilder.bind(processingDeadLetterQueue).to(processingDeadLetterExchange).with(PROCESSING_DLQ);
    }
}