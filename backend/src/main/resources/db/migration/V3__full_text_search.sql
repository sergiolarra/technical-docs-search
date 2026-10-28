-- Spanish configuration that strips accents before stemming, so "búsqueda", "busqueda"
-- and "BÚSQUEDAS" produce the same lexeme. Indexing, querying and highlighting all use it.
CREATE TEXT SEARCH CONFIGURATION es_unaccent (COPY = pg_catalog.spanish);
ALTER TEXT SEARCH CONFIGURATION es_unaccent
    ALTER MAPPING FOR hword, hword_part, word WITH unaccent, spanish_stem;

-- array_to_string() is STABLE, which generated columns do not allow. For text[] the result
-- only depends on the input, so this wrapper can safely be declared IMMUTABLE.
CREATE FUNCTION immutable_array_to_string(text[], text)
    RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
AS $$ SELECT array_to_string($1, $2) $$;

-- Weighted search vector, kept up to date by PostgreSQL on every insert/update:
--   A = title, B = author/category/tags, C = content (first 1,000,000 characters,
--   which keeps the tsvector under PostgreSQL's 1 MB limit).
ALTER TABLE documents
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('es_unaccent', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('es_unaccent',
                              coalesce(author, '') || ' ' || coalesce(category, '') || ' ' || immutable_array_to_string(tags, ' ')), 'B') ||
        setweight(to_tsvector('es_unaccent', left(coalesce(content, ''), 1000000)), 'C')
        ) STORED;

-- Inverted index, partial: only INDEXADO documents are searchable, so the index skips the rest.
CREATE INDEX idx_documents_search ON documents USING GIN (search_vector) WHERE status = 'INDEXADO';