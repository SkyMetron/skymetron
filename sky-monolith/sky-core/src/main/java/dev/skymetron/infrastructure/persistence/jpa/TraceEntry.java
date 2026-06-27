package dev.skymetron.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "trace_entries")
public class TraceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "trace_id", nullable = false)
    private UUID traceId;

    @Column(name = "agent_id", length = 100)
    private String agentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TraceEntry() {
    }

    public TraceEntry(UUID eventId, String eventType, UUID traceId, String agentId, Map<String, Object> payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.traceId = traceId;
        this.agentId = agentId;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getTraceId() { return traceId; }
    public String getAgentId() { return agentId; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
