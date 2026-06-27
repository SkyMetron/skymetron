package dev.skymetron.application.agent;

import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.observation.SecurityAnalysisEvent;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SecurityAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SecurityAgent.class);
    private static final AgentId SECURITY_ID = AgentId.of("00000000-0000-0000-0000-000000000009");

    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private final AtomicLong vulnerabilitiesFound = new AtomicLong(0);
    private double totalLatencyMs = 0;

    private static final List<VulnerabilityPattern> PATTERNS = List.of(
            new VulnerabilityPattern("SQL Injection", "CRITICAL",
                    List.of("executeQuery", "executeUpdate", "createStatement", "jdbc:")),
            new VulnerabilityPattern("Command Injection", "CRITICAL",
                    List.of("Runtime.exec", "ProcessBuilder", "exec(", "cmd.exe", "/bin/sh")),
            new VulnerabilityPattern("Hardcoded Secret", "HIGH",
                    List.of("password=", "apiKey=", "secret=", "token=", "aws_access_key")),
            new VulnerabilityPattern("XSS", "HIGH",
                    List.of("innerHTML", "outerHTML", "document.write", "eval(")),
            new VulnerabilityPattern("Path Traversal", "MEDIUM",
                    List.of("../", "..\\", "getAbsolutePath", "getCanonicalPath")),
            new VulnerabilityPattern("Weak Crypto", "MEDIUM",
                    List.of("MD5", "SHA1", "DES", "RC4")),
            new VulnerabilityPattern("Null Check Missing", "LOW",
                    List.of(".equals(", ".toString()", ".hashCode()"))
    );

    public record VulnerabilityPattern(String type, String severity, List<String> indicators) {}
    public record Finding(String type, String severity, String location, int line) {}

    public SecurityAgent(EventPublisher eventPublisher, SkyMetricsRegistry metrics) {
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Override
    public AgentId id() { return SECURITY_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(Intent.SECURITY_ANALYSIS),
                Set.of(),
                Set.of("security.scan")
        );
    }

    @Override
    public AgentStatus status() { return status; }

    @Override
    public CompletableFuture<AgentResponse> process(AgentRequest request) {
        status = AgentStatus.PROCESSING;
        totalRequests.incrementAndGet();
        long start = java.lang.System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = handleSecurityRequest(request.payload(), java.util.UUID.randomUUID());
                long elapsed = java.lang.System.currentTimeMillis() - start;
                successful.incrementAndGet();
                totalLatencyMs += elapsed;
                status = AgentStatus.IDLE;
                return AgentResponse.success(request, result, 0.85, Duration.ofMillis(elapsed));
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = java.lang.System.currentTimeMillis() - start;
                log.error("[SecurityAgent] error: {}", e.getMessage(), e);
                return AgentResponse.failure(request, "SecurityAgent error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    String handleSecurityRequest(String payload, UUID traceId) {
        if (payload == null || payload.isBlank()) {
            return "Security Agent. Usage: scan|targetName|code or status";
        }

        String lower = payload.toLowerCase().trim();

        if (lower.startsWith("scan|")) {
            String[] parts = payload.split("\\|", 3);
            if (parts.length < 3) return "Usage: scan|targetName|code";
            return scanCode(parts[1].trim(), parts[2].trim(), traceId);
        }

        if (lower.equals("status")) {
            return "Vulnerabilities found: " + vulnerabilitiesFound.get()
                    + " | Patterns active: " + PATTERNS.size();
        }

        if (lower.equals("patterns")) {
            StringBuilder sb = new StringBuilder("Active vulnerability patterns:\n");
            for (VulnerabilityPattern p : PATTERNS) {
                sb.append("  ").append(p.severity()).append(": ").append(p.type()).append("\n");
            }
            return sb.toString();
        }

        return "Unknown command: " + payload + ". Commands: scan, status, patterns";
    }

    String scanCode(String targetName, String code, UUID traceId) {
        List<Finding> findings = new ArrayList<>();
        String[] lines = code.split("\n");
        int lineNum = 0;

        for (String line : lines) {
            lineNum++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*")) {
                continue;
            }
            for (VulnerabilityPattern pattern : PATTERNS) {
                for (String indicator : pattern.indicators()) {
                    if (trimmed.contains(indicator)) {
                        findings.add(new Finding(pattern.type(), pattern.severity(), trimmed, lineNum));
                        break;
                    }
                }
            }
        }

        long critical = findings.stream().filter(f -> f.severity().equals("CRITICAL")).count();
        long high = findings.stream().filter(f -> f.severity().equals("HIGH")).count();
        long medium = findings.stream().filter(f -> f.severity().equals("MEDIUM")).count();
        long low = findings.stream().filter(f -> f.severity().equals("LOW")).count();

        vulnerabilitiesFound.addAndGet(findings.size());
        boolean passed = findings.isEmpty();
        String severity = passed ? "NONE" : critical > 0 ? "CRITICAL" : high > 0 ? "HIGH" : medium > 0 ? "MEDIUM" : "LOW";

        eventPublisher.publishSecurityAnalysis(SecurityAnalysisEvent.of(
                targetName, severity, findings.isEmpty() ? "none" : findings.get(0).type(),
                findings.size(), passed, traceId));

        StringBuilder sb = new StringBuilder();
        sb.append("=== Security Scan: ").append(targetName).append(" ===\n");
        sb.append("Result: ").append(passed ? "PASSED" : "FAILED").append("\n");
        sb.append("Severity: ").append(severity).append("\n");
        sb.append("Findings: ").append(findings.size()).append("\n");
        if (critical > 0) sb.append("  CRITICAL: ").append(critical).append("\n");
        if (high > 0) sb.append("  HIGH: ").append(high).append("\n");
        if (medium > 0) sb.append("  MEDIUM: ").append(medium).append("\n");
        if (low > 0) sb.append("  LOW: ").append(low).append("\n");
        if (!findings.isEmpty()) {
            sb.append("\nDetails:\n");
            for (Finding f : findings) {
                sb.append("  [").append(f.severity()).append("] ").append(f.type())
                        .append(" (line ").append(f.line).append("): ")
                        .append(f.location()).append("\n");
            }
        }

        log.info("[SecurityAgent] scan={} findings={} severity={}", targetName, findings.size(), severity);
        return sb.toString();
    }

    @Override
    public HealthStatus health() {
        long total = totalRequests.get();
        if (total == 0) return HealthStatus.HEALTHY;
        double rate = (double) successful.get() / total;
        if (rate > 0.8) return HealthStatus.HEALTHY;
        if (rate > 0.5) return HealthStatus.DEGRADED;
        return HealthStatus.UNHEALTHY;
    }

    @Override
    public AgentMetrics metrics() {
        long total = totalRequests.get();
        double avgLatency = total > 0 ? totalLatencyMs / total : 0;
        double rate = total > 0 ? (double) successful.get() / total : 0;
        return new AgentMetrics(total, successful.get(), failed.get(), avgLatency, rate);
    }
}
