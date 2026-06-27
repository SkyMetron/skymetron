package dev.skymetron.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final int maxRequestsPerMinute;
    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(@Value("${sky.security.rate-limit:120}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/auth/")
                || request.getRequestURI().equals("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveIp(request);
        RateBucket bucket = buckets.computeIfAbsent(ip, k -> new RateBucket());

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"too-many-requests\",\"message\":\"Rate limit exceeded. Try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    boolean tryConsume(String ip) {
        RateBucket bucket = buckets.computeIfAbsent(ip, k -> new RateBucket());
        return bucket.tryConsume();
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private class RateBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= maxRequestsPerMinute;
        }
    }
}
