package dev.skymetron.domain.execution;

/**
 * Identifies an entity — its name, type (agent, session, mission), and status.
 *
 * <p>Attached to every entity that should be addressable by name.
 */
public record IdentityComponent(
        String name,
        EntityType type,
        EntityStatus status
) implements Component {

    public IdentityComponent {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("IdentityComponent name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("IdentityComponent type must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("IdentityComponent status must not be null");
        }
    }

    public IdentityComponent withStatus(EntityStatus newStatus) {
        return new IdentityComponent(name, type, newStatus);
    }

    public enum EntityType {
        AGENT, SESSION, MISSION
    }

    public enum EntityStatus {
        ACTIVE, PAUSED, TERMINATED
    }
}
