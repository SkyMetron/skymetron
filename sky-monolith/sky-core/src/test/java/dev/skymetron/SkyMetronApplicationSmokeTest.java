package dev.skymetron;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Smoke test — verifies the application entry point is correctly configured.
 *
 * <p>Runs by default (no external services, no Spring context) so that
 * {@code mvn clean test} is green on a fresh checkout.
 */
class SkyMetronApplicationSmokeTest {

    @Test
    @DisplayName("SkyMetronApplication is annotated with @SpringBootApplication")
    void applicationIsAnnotated() {
        assertThat(SkyMetronApplication.class.isAnnotationPresent(SpringBootApplication.class))
                .isTrue();
    }

    @Test
    @DisplayName("SkyMetronApplication exposes a static main(String[]) entry point")
    void applicationHasMainMethod() throws NoSuchMethodException {
        Method main = SkyMetronApplication.class.getMethod("main", String[].class);
        assertThat(main).isNotNull();
        assertThat(java.lang.reflect.Modifier.isStatic(main.getModifiers())).isTrue();
    }
}
