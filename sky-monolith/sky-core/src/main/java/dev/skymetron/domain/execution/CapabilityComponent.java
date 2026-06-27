package dev.skymetron.domain.execution;

import java.util.Set;

/**
 * Declares what an entity (typically an agent) can do.
 *
 * <p>Capabilities are plain strings (e.g. {@code "memory.read"},
 * {@code "tool.execute"}, {@code "research.web"}) used by the CEO agent
 * for routing decisions.
 */
public record CapabilityComponent(
        Set<String> capabilities
) implements Component {

    public CapabilityComponent {
        if (capabilities == null) {
            throw new IllegalArgumentException("CapabilityComponent capabilities must not be null");
        }
        capabilities = Set.copyOf(capabilities);
    }

    public CapabilityComponent() {
        this(Set.of());
    }

    public boolean has(String capability) {
        return capabilities.contains(capability);
    }

    public CapabilityComponent add(String capability) {
        var next = new java.util.HashSet<>(capabilities);
        next.add(capability);
        return new CapabilityComponent(Set.copyOf(next));
    }
}
