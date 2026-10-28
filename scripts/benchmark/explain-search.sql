-- Execution plan of the search query used by the API. With selective terms it must show a
-- Bitmap Index Scan on idx_documents_search (the GIN index), never a pattern scan.
\set ON_ERROR_STOP on
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
WITH q AS (SELECT websearch_to_tsquery('es_unaccent', :'terms') AS query),
ranked AS (
    SELECT d.id, d.created_at,
           ts_rank_cd(d.search_vector, q.query, 1) AS score,
           count(*) OVER () AS total_hits
    FROM documents d CROSS JOIN q
    WHERE d.status = 'INDEXADO' AND d.search_vector @@ q.query
    ORDER BY score DESC, d.created_at DESC, d.id
    LIMIT 10
)
SELECT d.id, r.total_hits,
       ts_headline('es_unaccent', d.title, q.query),
       ts_headline('es_unaccent', left(coalesce(d.content, ''), 100000), q.query)
FROM ranked r
         JOIN documents d ON d.id = r.id
         CROSS JOIN q;