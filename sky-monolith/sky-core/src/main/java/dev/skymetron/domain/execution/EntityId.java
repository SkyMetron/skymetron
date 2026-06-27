package dev.skymetron.domain.execution;

import java.util.UUID;

/**
 * Strongly-typed identifier for an ECS entity.
 *
 * <p>Wraps a {@link UUID} to provide type safety at API boundaries.
 */
public record EntityId(UUID value) implements Comparable<EntityId> {

    public EntityId {
        if (value == null) {
            throw new IllegalArgumentException("EntityId value must not be null");
        }
    }

    public static EntityId create() {
        return new EntityId(UUID.randomUUID());
    }

    public static EntityId of(UUID value) {
        return new EntityId(value);
    }

    public static EntityId of(String value) {
        return new EntityId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int compareTo(EntityId other) {
        return this.value.compareTo(other.value);
    }
}
