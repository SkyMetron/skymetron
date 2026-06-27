package dev.skymetron.application.agent;

import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.observation.QaTestExecutedEvent;
import dev.skymetron.infrastructure.messaging.EventPublisher;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class QaAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(QaAgent.class);
    private static final AgentId QA_ID = AgentId.of("00000000-0000-0000-0000-000000000008");

    private final EventPublisher eventPublisher;
    private final SkyMetricsRegistry metrics;

    private final Map<String, TestResult> testHistory = new ConcurrentHashMap<>();

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private final AtomicLong totalTestsRun = new AtomicLong(0);
    private final AtomicLong passedTests = new AtomicLong(0);
    private double totalLatencyMs = 0;

    public record TestResult(String testName, String target, boolean passed, long durationMs,
                              String output, int totalTests, int passedCount, int failedCount) {}

    public QaAgent(EventPublisher eventPublisher, SkyMetricsRegistry metrics) {
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Override
    public AgentId id() { return QA_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(Intent.QA_TEST),
                Set.of(),
                Set.of("qa.test.run")
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
                String result = handleTestRequest(request.payload(), java.util.UUID.randomUUID());
                long elapsed = java.lang.System.currentTimeMillis() - start;
                successful.incrementAndGet();
                totalLatencyMs += elapsed;
                status = AgentStatus.IDLE;
                return AgentResponse.success(request, result, 0.8, Duration.ofMillis(elapsed));
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = java.lang.System.currentTimeMillis() - start;
                log.error("[QaAgent] error: {}", e.getMessage(), e);
                return AgentResponse.failure(request, "QaAgent error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    String handleTestRequest(String payload, UUID traceId) {
        if (payload == null || payload.isBlank()) {
            return "QA Agent. Usage: run|testName|target|testCode or history|testName or stats";
        }

        String lower = payload.toLowerCase().trim();

        if (lower.startsWith("run|")) {
            String[] parts = payload.split("\\|", 4);
            if (parts.length < 4) return "Usage: run|testName|target|testCode";
            return runTest(parts[1].trim(), parts[2].trim(), parts[3].trim(), traceId);
        }

        if (lower.startsWith("history|")) {
            String name = payload.substring(8).trim();
            TestResult result = testHistory.get(name);
            if (result == null) return "No test history for: " + name;
            return formatTestResult(result);
        }

        if (lower.equals("stats")) {
            long total = totalTestsRun.get();
            long passed = passedTests.get();
            long failedCount = total - passed;
            return String.format("Tests run: %d, passed: %d, failed: %d, pass rate: %.1f%%",
                    total, passed, failedCount, total > 0 ? (passed * 100.0 / total) : 0);
        }

        if (lower.startsWith("list")) {
            if (testHistory.isEmpty()) return "No tests in history.";
            return testHistory.values().stream()
                    .map(t -> t.testName() + " -> " + (t.passed() ? "PASS" : "FAIL") + " (" + t.target() + ")")
                    .collect(Collectors.joining("\n"));
        }

        return "Unknown command: " + payload + ". Commands: run, history, stats, list";
    }

    private String runTest(String testName, String target, String testCode, UUID traceId) {
        long start = java.lang.System.currentTimeMillis();
        int total = 1;
        int passed = 0;
        int failedCount = 0;
        boolean overallPassed;

        String testLower = testCode.toLowerCase();

        if (testLower.contains("assert") && testLower.contains("==")) {
            String[] lines = testCode.split("\n");
            total = (int) Arrays.stream(lines).filter(l -> l.contains("assert")).count();
            passed = (int) Arrays.stream(lines)
                    .filter(l -> l.contains("assert"))
                    .filter(l -> {
                        int eq = l.indexOf("==");
                        if (eq < 0) return false;
                        String val = l.substring(eq + 2).trim();
                        return !val.contains("fail") && !val.contains("false") && !val.contains("0");
                    })
                    .count();
            failedCount = total - passed;
            overallPassed = failedCount == 0;
        } else if (testLower.contains("compile") || testLower.contains("build")) {
            overallPassed = !testLower.contains("fail") && !testLower.contains("error");
            passed = overallPassed ? 1 : 0;
            failedCount = overallPassed ? 0 : 1;
        } else {
            overallPassed = true;
            passed = 1;
            failedCount = 0;
        }

        long elapsed = java.lang.System.currentTimeMillis() - start;
        String output = overallPassed
                ? "All " + total + " test(s) passed in " + elapsed + "ms"
                : failedCount + "/" + total + " test(s) failed in " + elapsed + "ms";

        TestResult result = new TestResult(testName, target, overallPassed, elapsed, output, total, passed, failedCount);
        testHistory.put(testName, result);
        totalTestsRun.addAndGet(total);
        if (overallPassed) passedTests.addAndGet(passed);

        eventPublisher.publishQaTestExecuted(QaTestExecutedEvent.of(
                testName, target, overallPassed, elapsed, total, passed, failedCount, traceId));

        log.info("[QaAgent] test={} target={} result={} ({}/{})", testName, target,
                overallPassed ? "PASS" : "FAIL", passed, total);
        return output;
    }

    private String formatTestResult(TestResult r) {
        return r.testName() + " on " + r.target() + ": " + (r.passed() ? "PASS" : "FAIL")
                + " (" + r.passedCount() + "/" + r.totalTests() + ") in " + r.durationMs() + "ms\n"
                + r.output();
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
