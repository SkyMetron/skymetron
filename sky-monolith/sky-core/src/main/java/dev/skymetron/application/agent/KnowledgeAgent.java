package dev.skymetron.application.agent;

import dev.skymetron.domain.execution.*;
import dev.skymetron.domain.knowledge.Skill;
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
public class KnowledgeAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAgent.class);
    private static final AgentId KNOWLEDGE_ID = AgentId.of("00000000-0000-0000-0000-000000000007");

    private final Map<String, Skill> catalog = new ConcurrentHashMap<>();
    private final SkyMetricsRegistry metrics;

    private volatile AgentStatus status = AgentStatus.IDLE;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successful = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);
    private double totalLatencyMs = 0;

    public KnowledgeAgent(SkyMetricsRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public AgentId id() { return KNOWLEDGE_ID; }

    @Override
    public AgentCapabilities capabilities() {
        return new AgentCapabilities(
                Set.of(Intent.SKILL_MANAGEMENT),
                Set.of(),
                Set.of("skill.read", "skill.write")
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
                String result = handleSkillRequest(request.payload());
                long elapsed = java.lang.System.currentTimeMillis() - start;
                successful.incrementAndGet();
                totalLatencyMs += elapsed;
                status = AgentStatus.IDLE;
                return AgentResponse.success(request, result, 0.9, Duration.ofMillis(elapsed));
            } catch (Exception e) {
                failed.incrementAndGet();
                status = AgentStatus.ERROR;
                long elapsed = java.lang.System.currentTimeMillis() - start;
                log.error("[KnowledgeAgent] error: {}", e.getMessage(), e);
                return AgentResponse.failure(request, "KnowledgeAgent error: " + e.getMessage(),
                        Duration.ofMillis(elapsed));
            }
        });
    }

    String handleSkillRequest(String payload) {
        if (payload == null || payload.isBlank()) {
            return "Skills available: " + String.join(", ", catalog.keySet());
        }

        String lower = payload.toLowerCase().trim();

        if (lower.startsWith("add|")) {
            String[] parts = payload.split("\\|", 5);
            if (parts.length < 5) return "Usage: add|name|description|category|content";
            Skill skill = Skill.create(parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim());
            catalog.put(skill.name().toLowerCase(), skill);
            log.info("[KnowledgeAgent] added skill: {} (v{})", skill.name(), skill.version());
            return "Skill '" + skill.name() + "' added (v" + skill.version() + ", confidence=" + skill.confidence() + ")";
        }

        if (lower.startsWith("get|")) {
            String name = payload.substring(4).trim().toLowerCase();
            Skill skill = catalog.get(name);
            if (skill == null) return "Skill not found: " + name;
            return skill.name() + " (v" + skill.version() + "): " + skill.description()
                    + " | confidence=" + skill.confidence() + " | category=" + skill.category();
        }

        if (lower.startsWith("update|")) {
            String[] parts = payload.split("\\|", 3);
            if (parts.length < 3) return "Usage: update|name|content";
            String name = parts[1].trim().toLowerCase();
            Skill existing = catalog.get(name);
            if (existing == null) return "Skill not found: " + name;
            Skill updated = existing.withVersion(existing.version() + 1);
            catalog.put(name, new Skill(updated.id(), updated.name(), updated.description(),
                    updated.category(), parts[2].trim(), updated.version(), updated.confidence(),
                    updated.createdAt(), updated.updatedAt()));
            log.info("[KnowledgeAgent] updated skill: {} (v{})", name, updated.version());
            return "Skill '" + name + "' updated to v" + updated.version();
        }

        if (lower.startsWith("confidence|")) {
            String[] parts = payload.split("\\|", 3);
            if (parts.length < 3) return "Usage: confidence|name|score";
            String name = parts[1].trim().toLowerCase();
            double score = Double.parseDouble(parts[2].trim());
            Skill existing = catalog.get(name);
            if (existing == null) return "Skill not found: " + name;
            catalog.put(name, existing.withConfidence(Math.max(0, Math.min(1, score))));
            return "Skill '" + name + "' confidence set to " + score;
        }

        if (lower.startsWith("list")) {
            if (catalog.isEmpty()) return "No skills in catalog.";
            return catalog.values().stream()
                    .map(s -> s.name() + " (v" + s.version() + ", conf=" + s.confidence() + ")")
                    .collect(Collectors.joining("\n"));
        }

        if (lower.startsWith("delete|")) {
            String name = payload.substring(7).trim().toLowerCase();
            Skill removed = catalog.remove(name);
            if (removed == null) return "Skill not found: " + name;
            log.info("[KnowledgeAgent] deleted skill: {}", name);
            return "Skill '" + name + "' deleted";
        }

        return "Unknown command: " + payload + ". Commands: add, get, update, confidence, list, delete";
    }

    public int skillCount() { return catalog.size(); }
    public Optional<Skill> getSkill(String name) { return Optional.ofNullable(catalog.get(name.toLowerCase())); }

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
