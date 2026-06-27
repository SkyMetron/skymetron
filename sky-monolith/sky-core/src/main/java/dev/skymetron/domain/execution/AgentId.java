package dev.skymetron.domain.execution;

import java.util.UUID;

/**
 * Strongly-typed identifier for an agent.
 */
public record AgentId(UUID value) implements Comparable<AgentId> {

    public AgentId {
        if (value == null) {
            throw new IllegalArgumentException("AgentId value must not be null");
        }
    }

    public static AgentId create() {
        return new AgentId(UUID.randomUUID());
    }

    public static AgentId of(String value) {
        return new AgentId(UUID.fromString(value));
    }

    @Override
    public int compareTo(AgentId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
