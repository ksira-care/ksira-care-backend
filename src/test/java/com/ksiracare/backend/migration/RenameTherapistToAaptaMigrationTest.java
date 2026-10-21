package com.ksiracare.backend.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** V3 must carry existing production rows across the therapist → aapta rename, not just create the schema. */
class RenameTherapistToAaptaMigrationTest {

    private static final String URL =
            "jdbc:h2:mem:v3-%s;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1".formatted(UUID.randomUUID());

    private static Flyway flywayUpTo(String version) {
        return Flyway.configure().dataSource(URL, "sa", "").target(version).load();
    }

    @Test
    void renamesTablesAndColumnsAndKeepsTheData() throws SQLException {
        flywayUpTo("2").migrate();
        try (Connection db = DriverManager.getConnection(URL, "sa", ""); Statement sql = db.createStatement()) {
            sql.execute("""
                    INSERT INTO therapists (id, first_name, last_name, email, password, created_at, updated_at)
                    VALUES ('11111111-1111-1111-1111-111111111111', 'Aanya', 'Mehta', 'a@k.com', 'hash', now(), now())""");
            sql.execute("""
                    INSERT INTO therapist_languages (therapist_id, language_id)
                    SELECT '11111111-1111-1111-1111-111111111111', id FROM languages WHERE code = 'HINDI'""");
            sql.execute("""
                    INSERT INTO slots (id, therapist_id, slot_time, status, created_at, updated_at) VALUES
                    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', now(), 'THERAPIST_AVAILABLE', now(), now()),
                    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', now() + INTERVAL '1' HOUR, 'THERAPIST_UNAVAILABLE', now(), now()),
                    ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', now() + INTERVAL '2' HOUR, 'BOOKED', now(), now())""");
            sql.execute("""
                    INSERT INTO bookings (id, therapist_id, customer_name, start_time, end_time, status, created_at, updated_at)
                    VALUES ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111',
                            'Sara', now(), now(), 'THERAPIST_NO_SHOW', now(), now())""");
        }

        flywayUpTo("3").migrate();

        try (Connection db = DriverManager.getConnection(URL, "sa", ""); Statement sql = db.createStatement()) {
            assertThat(single(sql, "SELECT email FROM aaptas")).isEqualTo("a@k.com");
            assertThat(single(sql, "SELECT COUNT(*) FROM aapta_languages WHERE aapta_id = '11111111-1111-1111-1111-111111111111'"))
                    .isEqualTo("1");
            assertThat(single(sql, "SELECT STRING_AGG(status, ',' ORDER BY slot_time) FROM slots WHERE aapta_id IS NOT NULL"))
                    .isEqualTo("AAPTA_AVAILABLE,AAPTA_UNAVAILABLE,BOOKED");
            assertThat(single(sql, "SELECT status FROM bookings WHERE aapta_id IS NOT NULL")).isEqualTo("AAPTA_NO_SHOW");
        }
    }

    private static String single(Statement sql, String query) throws SQLException {
        try (ResultSet row = sql.executeQuery(query)) {
            assertThat(row.next()).isTrue();
            return row.getString(1);
        }
    }
}
