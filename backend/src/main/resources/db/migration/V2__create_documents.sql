CREATE TABLE documents (
                           id                UUID          PRIMARY KEY,
                           title             VARCHAR(255)  NOT NULL,
                           author            VARCHAR(150)  NOT NULL,
                           category          VARCHAR(100)  NOT NULL,
                           tags              TEXT[]        NOT NULL DEFAULT '{}',
                           version           VARCHAR(50)   NOT NULL,
                           original_filename VARCHAR(255)  NOT NULL,
                           format            VARCHAR(20)   NOT NULL,
                           content_type      VARCHAR(100)  NOT NULL,
                           file_size         BIGINT        NOT NULL,
                           storage_key       VARCHAR(255)  NOT NULL,
                           content           TEXT,
                           status            VARCHAR(20)   NOT NULL,
                           error_message     VARCHAR(500),
                           created_at        TIMESTAMPTZ   NOT NULL,
                           updated_at        TIMESTAMPTZ   NOT NULL,
                           CONSTRAINT chk_documents_status CHECK (status IN ('PROCESANDO', 'INDEXADO', 'ERROR')),
                           CONSTRAINT chk_documents_format CHECK (format IN ('PDF', 'TXT', 'MARKDOWN'))
);

CREATE INDEX idx_documents_status     ON documents (status);
CREATE INDEX idx_documents_category   ON documents (category);
CREATE INDEX idx_documents_tags       ON documents USING GIN (tags);
CREATE INDEX idx_documents_created_at ON documents (created_at DESC);