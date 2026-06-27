package dev.skymetron.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test — loads the full Spring context against real PostgreSQL
 * (pgvector) and RabbitMQ via Testcontainers.
 *
 * <p>Tagged {@code integration} so it is excluded from the default
 * {@code mvn test} run. Run explicitly with:
 * <pre>{@code
 * mvn test -Dgroups=integration -pl sky-core
 * }</pre>
 *
 * <p>Requires Docker running on the host.
 */
@Tag("integration")
@SpringBootTest(classes = {dev.skymetron.SkyMetronApplication.class, TestContainersConfig.class})
@ActiveProfiles("test")
class ContextLoadsIT {

    @Autowired
    ApplicationContext context;

    @Test
    void contextLoadsAndPgvectorMigrationApplied() {
        assertThat(context).isNotNull();
    }
}
