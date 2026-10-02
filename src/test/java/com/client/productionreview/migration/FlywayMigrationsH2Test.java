package com.client.productionreview.migration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Os testes usam o schema gerado pelo Hibernate; este teste garante que as migrações (até a V4) também
 * rodam no H2 em modo MySQL e criam as colunas que as entidades novas usam.
 */
class FlywayMigrationsH2Test {

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-compat;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            // a V1 cria/seleciona o banco (só faz sentido no MariaDB); o resto roda igual
            String v1 = new String(new ClassPathResource("db/migration/V1__create.sql").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8).replaceAll("(?im)^\\s*(CREATE DATABASE|use)\\b.*$", "");
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(v1.getBytes(StandardCharsets.UTF_8)));
            for (String script : List.of("V2__create_table_profile.sql", "V3__review_moderation_and_helpful.sql",
                    "V4__search_media_reports_notifications.sql")) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/" + script));
            }
        }
    }

    private static Set<String> columns(String table) {
        return jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE LOWER(TABLE_NAME) = ?",
                        String.class, table).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Test
    void v4_createsTablesAndColumnsUsedByTheEntities() {
        assertTrue(columns("product").contains("search_name"));
        assertTrue(columns("user").contains("email_notifications"));
        assertTrue(columns("review").containsAll(Set.of("reply_text", "reply_author_id", "replied_at")));
        assertEquals(Set.of("id", "review_id", "object_key", "content_type", "size_bytes", "created_at"), columns("review_image"));
        assertEquals(Set.of("id", "review_id", "user_id", "reason", "details", "created_at"), columns("review_report"));
        assertEquals(Set.of("id", "user_id", "type", "title", "message", "link", "review_id", "is_read", "created_at"),
                columns("notification"));
        assertEquals(Set.of("product_id", "user_id", "created_at"), columns("product_follow"));
    }

    @Test
    void v4_defaultsAndConstraints() {
        jdbc.update("INSERT INTO user (name, email, username, password, active) VALUES ('a', 'a@a', 'a', 'x', TRUE)");
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT email_notifications FROM user WHERE username = 'a'", Boolean.class));

        jdbc.update("INSERT INTO category (name, description, slug) VALUES ('c', 'd', 'c')");
        jdbc.update("INSERT INTO sub_category (name, description, slug, category_id) VALUES ('s', 'd', 's', "
                + "(SELECT id FROM category WHERE slug = 'c'))");
        jdbc.update("INSERT INTO product (name, description, slug, sub_category_id) VALUES ('p', 'd', 'p', "
                + "(SELECT id FROM sub_category WHERE slug = 's'))");
        jdbc.update("INSERT INTO review (product_id, user_id, title, description, note) VALUES ("
                + "(SELECT id FROM product WHERE slug = 'p'), (SELECT id FROM user WHERE username = 'a'), 't', 'd', 5)");
        String reportSql = "INSERT INTO review_report (review_id, user_id, reason) VALUES ((SELECT MAX(id) FROM review), "
                + "(SELECT id FROM user WHERE username = 'a'), 'SPAM')";
        jdbc.update(reportSql);
        // uma denúncia por usuário e review
        assertThrows(Exception.class, () -> jdbc.update(reportSql));

        jdbc.update("INSERT INTO notification (user_id, type, title, message) VALUES ("
                + "(SELECT id FROM user WHERE username = 'a'), 'REVIEW_HELPFUL', 't', 'm')");
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT is_read FROM notification", Boolean.class));

        // detalhes da denúncia até 500 e resposta até 1000 caracteres
        jdbc.update("UPDATE review_report SET details = ?", "x".repeat(500));
        jdbc.update("UPDATE review SET reply_text = ?", "y".repeat(1000));
    }
}
