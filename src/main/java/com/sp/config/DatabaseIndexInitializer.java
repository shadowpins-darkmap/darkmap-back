package com.sp.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures critical indexes exist in production databases where DDL auto-update is disabled.
 * Without these indexes, social login queries degrade to table scans.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseIndexInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureIndexes() {
        createUniqueIndexIfMissing("member", "idx_member_email", "email");
        createUniqueIndexIfMissing("member", "idx_member_member_id", "member_id");
        createIndexIfMissing("member", "idx_member_user_number", "user_number");
    }

    private void createUniqueIndexIfMissing(String tableName, String indexName, String columnName) {
        createIndex(tableName, indexName, columnName, true);
    }

    private void createIndexIfMissing(String tableName, String indexName, String columnName) {
        createIndex(tableName, indexName, columnName, false);
    }

    private void createIndex(String tableName, String indexName, String columnName, boolean unique) {
        if (indexExists(tableName, indexName)) {
            return;
        }

        String sql = String.format("CREATE %sINDEX %s ON %s (%s)",
                unique ? "UNIQUE " : "",
                indexName,
                tableName,
                columnName);

        try {
            jdbcTemplate.execute(sql);
            log.info("Created {} index {} on {}({})", unique ? "unique" : "", indexName, tableName, columnName);
        } catch (Exception e) {
            log.error("Failed to create index {} on {}({})", indexName, tableName, columnName, e);
        }
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }
}
