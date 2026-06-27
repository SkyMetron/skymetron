package dev.skymetron.it;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers wiring for integration tests.
 *
 * <p>Provides real PostgreSQL (pgvector) and RabbitMQ containers auto-wired via
 * {@link ServiceConnection}. Redis is excluded in the test profile to keep the
 * container set minimal — cache is non-critical for Sprint 0 context-load tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(
                DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("skymetron_test")
                .withUsername("skymetron")
                .withPassword("skymetron");
    }

    @Bean
    @ServiceConnection
    public RabbitMQContainer rabbitmq() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));
    }
}
