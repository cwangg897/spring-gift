package gift.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Flyway V1/V2 migrations apply against the Testcontainers MySQL
 * instance. This is the V3-2 (c) acceptance criterion for the 00-test-infra PR
 * and serves as the canary for AbstractIntegrationTest itself.
 */
class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayHistoryContainsV1AndV2() {
        Integer applied = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success = true",
            Integer.class
        );

        assertThat(applied).isNotNull();
        assertThat(applied).isGreaterThanOrEqualTo(2);
    }

    @Test
    void seedDataFromV2IsPresent() {
        Integer categoryCount = jdbcTemplate.queryForObject(
            "select count(*) from category",
            Integer.class
        );
        Integer productCount = jdbcTemplate.queryForObject(
            "select count(*) from product",
            Integer.class
        );

        assertThat(categoryCount).isNotNull().isGreaterThanOrEqualTo(3);
        assertThat(productCount).isNotNull().isGreaterThanOrEqualTo(6);
    }
}
