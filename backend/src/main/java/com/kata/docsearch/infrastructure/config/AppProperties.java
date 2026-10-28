package com.kata.docsearch.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Application-specific configuration; validated at startup (fails fast if anything is missing). */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(Storage storage, Processing processing) {

    public record Storage(@NotBlank String path) {
    }

    public record Processing(@Positive int maxExtractedChars) {
    }
}