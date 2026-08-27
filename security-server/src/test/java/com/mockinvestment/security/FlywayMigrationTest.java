package com.mockinvestment.security;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway 마이그레이션 검증 테스트.
 * Testcontainers로 실제 MySQL 8.4 컨테이너를 띄워 마이그레이션을 적용한다.
 * (로컬 MySQL 상태와 무관하게 항상 같은 조건에서 검증 — Docker 필요)
 */
@Testcontainers
class FlywayMigrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("moa_security");

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .load();
    }

    @Test
    void 마이그레이션이_성공하고_모든_테이블이_생성된다() throws Exception {
        MigrateResult result = flyway().migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(1);

        try (Connection conn = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData()
                    .getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            assertThat(tables).contains(
                    "users", "authentication", "stock", "market_status",
                    "cash_wallet", "cash_wallet_history",
                    "stock_wallet", "stock_wallet_history",
                    "orders", "matches",
                    "flyway_schema_history"
            );
        }
    }

    @Test
    void CHECK_제약이_동작한다_묶인_금액이_예치금을_초과하면_거부() throws Exception {
        flyway().migrate();

        try (Connection conn = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
            conn.createStatement().executeUpdate(
                    "INSERT INTO users (email, password, created_at, updated_at) " +
                    "VALUES ('t@t.com', 'x', NOW(6), NOW(6))");

            // tied_balance(20000) > balance(10000) → CHECK 제약 위반이어야 한다
            var thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                    conn.createStatement().executeUpdate(
                            "INSERT INTO cash_wallet (user_id, account_no, balance, tied_balance, created_at, updated_at) " +
                            "VALUES (1, 'enc', 10000, 20000, NOW(6), NOW(6))"));

            assertThat(thrown)
                    .isInstanceOf(java.sql.SQLException.class)
                    .hasMessageContaining("chk_cash_wallet_tied");
        }
    }
}
