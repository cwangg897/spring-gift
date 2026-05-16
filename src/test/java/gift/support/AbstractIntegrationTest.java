package gift.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base class for integration tests that need a real MySQL database.
 *
 * Uses a singleton MySQL Testcontainer started once per JVM via a static
 * initializer (not via @Container so its lifecycle is not tied to any single
 * test class). Flyway runs V1/V2 against this container on context load.
 *
 * Default isolation: Spring rolls back any @Transactional test method. For
 * AFTER_COMMIT event verification (ADR-006a), subclasses must omit
 * @Transactional and use @Sql cleanup instead.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
        .withDatabaseName("spring_gift_test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
