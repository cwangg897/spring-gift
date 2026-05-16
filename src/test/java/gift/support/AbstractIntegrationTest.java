package gift.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real MySQL database.
 *
 * Uses a singleton MySQL Testcontainer reused across all test classes that
 * extend this base, keeping CI time bounded. Spring Boot's @ServiceConnection
 * wires the container into the application's datasource automatically, so
 * Flyway migrations run against the container on context load.
 *
 * Default isolation: Spring rolls back any @Transactional test method. For
 * AFTER_COMMIT event verification (ADR-006a), subclasses must omit
 * @Transactional and use @Sql cleanup instead.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
        .withDatabaseName("spring_gift_test")
        .withReuse(true);
}
