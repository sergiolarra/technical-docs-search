package com.kata.docsearch.application.port.out;

import java.io.InputStream;

public interface ContentTypeDetectorPort {
    /** Actual MIME type based on the content (magic bytes), not on what the client claims. */
    String detect(InputStream content, String filename);
}
