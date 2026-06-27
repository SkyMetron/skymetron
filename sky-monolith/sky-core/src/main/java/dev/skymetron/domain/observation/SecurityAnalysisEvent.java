package dev.skymetron.domain.observation;

import java.util.UUID;

public record SecurityAnalysisEvent(
        UUID eventId,
        java.time.Instant timestamp,
        UUID traceId,
        String target,
        String severity,
        String vulnerabilityType,
        int issueCount,
        boolean passed
) {
    public static final String ROUTING_KEY = "security.analysis";

    public static SecurityAnalysisEvent of(String target, String severity, String vulnerabilityType,
                                            int issueCount, boolean passed, UUID traceId) {
        return new SecurityAnalysisEvent(UUID.randomUUID(), java.time.Instant.now(), traceId,
                target, severity, vulnerabilityType, issueCount, passed);
    }
}
