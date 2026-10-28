package com.kata.docsearch.application.port.in;

import java.io.IOException;
import java.io.InputStream;

/** Allows the content to be opened more than once: first to detect its type, and then to save it. */
@FunctionalInterface
public interface ContentSource {
    InputStream open() throws IOException;
}
