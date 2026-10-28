package com.kata.docsearch.infrastructure.adapter.out.search;

import com.kata.docsearch.application.port.out.SearchEnginePort;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.SearchHit;
import com.kata.docsearch.domain.model.SearchQuery;
import com.kata.docsearch.domain.model.SearchResultPage;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.kata.docsearch.infrastructure.adapter.out.search.HeadlineParser.MATCH_END;
import static com.kata.docsearch.infrastructure.adapter.out.search.HeadlineParser.MATCH_START;

/**
 * PostgreSQL full-text search: websearch_to_tsquery + partial GIN index + ts_rank_cd.
 * ts_headline is expensive (it re-parses the text), so it only runs on the rows of the requested page.
 */
@Component
@RequiredArgsConstructor
class PostgresSearchEngineAdapter implements SearchEnginePort {

    private static final String TS_CONFIG = "es_unaccent";

    /** Only the first characters are scanned for the snippet, keeping headline cost bounded. */
    private static final int SNIPPET_SOURCE_CHARS = 100_000;

    private static final String MARKERS = "StartSel=\"" + MATCH_START + "\", StopSel=\"" + MATCH_END + "\"";
    private static final String TITLE_OPTIONS = "HighlightAll=true, " + MARKERS;
    private static final String SNIPPET_OPTIONS =
            "MaxFragments=2, MinWords=10, MaxWords=30, FragmentDelimiter=\" … \", " + MARKERS;

    private final NamedParameterJdbcTemplate jdbc;

    /** A hit plus the total number of matches, computed by the window function in the same query. */
    private record Row(SearchHit hit, long totalHits) {
    }

    @Override
    public SearchResultPage search(SearchQuery query) {
        var params = new MapSqlParameterSource()
                .addValue("text", query.text())
                .addValue("limit", query.size())
                .addValue("offset", query.offset())
                .addValue("titleOptions", TITLE_OPTIONS)
                .addValue("snippetOptions", SNIPPET_OPTIONS)
                .addValue("snippetChars", SNIPPET_SOURCE_CHARS);
        String filters = filters(query, params);

        // Ranking only carries narrow columns (id, date, score); title and content are read just for
        // the rows of the requested page. Carrying content for every match made the window spill to disk.
        // count(*) OVER () counts every match while they are ranked, so the total comes for free.
        String sql = """
                WITH q AS (SELECT websearch_to_tsquery('%1$s', :text) AS query),
                ranked AS (
                    SELECT d.id, d.created_at,
                           ts_rank_cd(d.search_vector, q.query, 1) AS score,
                           count(*) OVER () AS total_hits
                    FROM documents d CROSS JOIN q
                    WHERE d.status = 'INDEXADO'
                      AND d.search_vector @@ q.query
                      %2$s
                    ORDER BY score DESC, d.created_at DESC, d.id
                    LIMIT :limit OFFSET :offset
                )
                SELECT d.id, d.author, d.category, d.tags, d.version, d.format, d.created_at, r.score, r.total_hits,
                       ts_headline('%1$s', d.title, q.query, :titleOptions) AS title_headline,
                       ts_headline('%1$s', left(coalesce(d.content, ''), :snippetChars), q.query, :snippetOptions) AS snippet_headline
                FROM ranked r
                JOIN documents d ON d.id = r.id
                CROSS JOIN q
                ORDER BY r.score DESC, r.created_at DESC, r.id
                """.formatted(TS_CONFIG, filters);

        List<Row> rows = jdbc.query(sql, params, this::mapRow);
        long total = rows.isEmpty() ? totalForEmptyPage(query, filters, params) : rows.getFirst().totalHits();
        return new SearchResultPage(rows.stream().map(Row::hit).toList(), query.page(), query.size(), total);
    }

    /** An empty first page means no matches; an empty later page (past the end) needs a real count. */
    private long totalForEmptyPage(SearchQuery query, String filters, MapSqlParameterSource params) {
        return query.offset() == 0 ? 0 : count(filters, params);
    }

    /** Only fixed SQL fragments are appended; every user value is a bound parameter. */
    private static String filters(SearchQuery query, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder();
        if (query.category() != null) {
            sql.append(" AND lower(d.category) = lower(:category)");
            params.addValue("category", query.category());
        }
        if (!query.tags().isEmpty()) {
            sql.append(" AND d.tags @> string_to_array(:tags, ',')");
            params.addValue("tags", String.join(",", query.tags()));
        }
        return sql.toString();
    }

    /** A page shorter than the page size is the last one, so the total is known without COUNT(*). */
    private static boolean isLastPage(SearchQuery query, List<SearchHit> hits) {
        return hits.size() < query.size() && (!hits.isEmpty() || query.offset() == 0);
    }

    private long count(String filters, MapSqlParameterSource params) {
        String sql = """
                SELECT count(*) FROM documents d
                WHERE d.status = 'INDEXADO'
                  AND d.search_vector @@ websearch_to_tsquery('%s', :text)
                  %s
                """.formatted(TS_CONFIG, filters);
        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total == null ? 0 : total;
    }

    private Row mapRow(ResultSet rs, int rowNum) throws SQLException {
        var hit = new SearchHit(
                rs.getObject("id", UUID.class),
                HeadlineParser.parse(rs.getString("title_headline")),
                rs.getString("author"),
                rs.getString("category"),
                List.of((String[]) rs.getArray("tags").getArray()),
                rs.getString("version"),
                DocumentFormat.valueOf(rs.getString("format")),
                rs.getDouble("score"),
                HeadlineParser.parse(rs.getString("snippet_headline")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
        return new Row(hit, rs.getLong("total_hits"));
    }
}