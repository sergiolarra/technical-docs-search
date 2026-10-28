package com.kata.docsearch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforces the "LIKE is strictly forbidden" requirement: fails the build if production code
 * uses LIKE/ILIKE/~~ in SQL or Spring Data derived queries that generate LIKE.
 */
class NoLikeOperatorTest {

    // SQL usage: "like '...'", "ilike :param", "like ?", "like concat(...)" and the ~~ operators
    private static final Pattern SQL_LIKE = Pattern.compile("(?i)\\bi?like\\s*['(:?]|\\bi?like\\s+concat|~~\\*?");

    // Spring Data derived queries that translate to LIKE
    private static final Pattern DERIVED_LIKE =
            Pattern.compile("\\b(find|count|exists|delete|stream)\\w*(Like|Containing|StartingWith|EndingWith)\\w*\\s*\\(");

    @Test
    void productionCodeNeverUsesLike() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main"))) {
            List<String> offenders = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".sql"))
                    .filter(NoLikeOperatorTest::usesLike)
                    .map(Path::toString)
                    .toList();

            assertThat(offenders).as("LIKE is forbidden by the requirements; use full-text search").isEmpty();
        }
    }

    private static boolean usesLike(Path path) {
        try {
            String source = Files.readString(path);
            return SQL_LIKE.matcher(source).find() || DERIVED_LIKE.matcher(source).find();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}