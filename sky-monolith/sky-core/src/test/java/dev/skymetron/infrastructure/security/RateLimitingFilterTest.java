package dev.skymetron.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitingFilter tests")
class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(5);
    }

    @Test
    @DisplayName("allows requests under the limit")
    void allowsUnderLimit() {
        boolean allowed = true;
        for (int i = 0; i < 5; i++) {
            allowed = filter.tryConsume("test-ip");
        }
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("blocks requests over the limit")
    void blocksOverLimit() {
        for (int i = 0; i < 5; i++) {
            filter.tryConsume("test-ip");
        }
        assertThat(filter.tryConsume("test-ip")).isFalse();
    }

    @Test
    @DisplayName("different IPs have independent counters")
    void independentIps() {
        for (int i = 0; i < 5; i++) {
            filter.tryConsume("ip-a");
        }
        assertThat(filter.tryConsume("ip-a")).isFalse();
        assertThat(filter.tryConsume("ip-b")).isTrue();
    }
}
