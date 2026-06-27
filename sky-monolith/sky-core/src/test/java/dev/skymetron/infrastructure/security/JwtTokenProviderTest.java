package dev.skymetron.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider tests")
class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(
            "test-secret-key-that-is-at-least-32-characters-long!!", 3600000);

    @Test
    @DisplayName("createToken() generates a valid token")
    void createToken() {
        String token = provider.createToken("admin", List.of("ADMIN", "USER"));
        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("admin");
        assertThat(provider.getRoles(token)).contains("ADMIN", "USER");
    }

    @Test
    @DisplayName("validateToken() returns false for expired token")
    void validateExpired() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-32-characters-long!!", 1);
        String token = shortLived.createToken("user", List.of("USER"));
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}
        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken() returns false for invalid token")
    void validateInvalid() {
        assertThat(provider.validateToken("invalid.token.here")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }
}
