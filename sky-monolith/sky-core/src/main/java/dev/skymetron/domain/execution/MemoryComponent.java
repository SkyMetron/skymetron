package dev.skymetron.domain.execution;

import java.util.Set;
import java.util.UUID;

/**
 * Links an entity to its memories in the Vault.
 *
 * <p>Holds references to {@code memory_entries} UUIDs that are relevant to
 * this entity (e.g. an agent's accumulated context, a session's facts).
 */
public record MemoryComponent(
        Set<UUID> memoryRefs
) implements Component {

    public MemoryComponent {
        if (memoryRefs == null) {
            throw new IllegalArgumentException("MemoryComponent memoryRefs must not be null");
        }
        memoryRefs = Set.copyOf(memoryRefs);
    }

    public MemoryComponent() {
        this(Set.of());
    }

    public MemoryComponent add(UUID memoryId) {
        var next = new java.util.HashSet<>(memoryRefs);
        next.add(memoryId);
        return new MemoryComponent(Set.copyOf(next));
    }

    public MemoryComponent remove(UUID memoryId) {
        var next = new java.util.HashSet<>(memoryRefs);
        next.remove(memoryId);
        return new MemoryComponent(Set.copyOf(next));
    }
}
