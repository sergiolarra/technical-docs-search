package com.kata.docsearch.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled, used by the SSE heartbeat. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}