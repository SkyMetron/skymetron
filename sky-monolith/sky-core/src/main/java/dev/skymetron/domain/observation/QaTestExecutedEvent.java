package dev.skymetron.domain.observation;

import java.util.UUID;

public record QaTestExecutedEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        String testName,
        String target,
        boolean passed,
        long durationMs,
        int totalTests,
        int passedTests,
        int failedTests
) {
    public static final String ROUTING_KEY = "qa.test.executed";

    public static QaTestExecutedEvent of(String testName, String target, boolean passed,
                                          long durationMs, int totalTests, int passedTests,
                                          int failedTests, UUID traceId) {
        return new QaTestExecutedEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                testName, target, passed, durationMs, totalTests, passedTests, failedTests);
    }
}
