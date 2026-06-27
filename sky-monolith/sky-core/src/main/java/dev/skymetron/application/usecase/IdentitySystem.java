package dev.skymetron.application.usecase;

import dev.skymetron.domain.execution.CapabilityComponent;
import dev.skymetron.domain.execution.EntityId;
import dev.skymetron.domain.execution.EntityStore;
import dev.skymetron.domain.execution.IdentityComponent;
import dev.skymetron.domain.execution.System;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * System that creates and manages entity identities.
 *
 * <p>The first functional ECS system. Provides convenience methods for
 * creating named entities (agents, sessions, missions) with an
 * {@link IdentityComponent}, and for transitioning their status.
 *
 * <p>The {@link #execute(EntityStore)} method scans all entities with an
 * IdentityComponent and logs a summary — a minimal but real system loop
 * that proves the ECS pipeline end-to-end.
 */
@Component
public class IdentitySystem implements System {

    private static final Logger log = LoggerFactory.getLogger(IdentitySystem.class);

    /**
     * Create a new entity with an identity and optional capabilities.
     *
     * @return the new entity id
     */
    public EntityId createIdentity(EntityStore store, String name,
                                   IdentityComponent.EntityType type,
                                   Set<String> capabilities) {
        EntityId id = store.createEntity();
        store.attachComponent(id, new IdentityComponent(name, type, IdentityComponent.EntityStatus.ACTIVE));
        if (capabilities != null && !capabilities.isEmpty()) {
            store.attachComponent(id, new CapabilityComponent(capabilities));
        }
        log.info("Created identity: name={} type={} id={}", name, type, id);
        return id;
    }

    /**
     * Transition an entity's status (e.g. ACTIVE → PAUSED).
     */
    public void updateStatus(EntityStore store, EntityId entityId,
                             IdentityComponent.EntityStatus newStatus) {
        Optional<IdentityComponent> current = store.getComponent(entityId, IdentityComponent.class);
        if (current.isEmpty()) {
            throw new IllegalArgumentException("Entity has no IdentityComponent: " + entityId);
        }
        store.attachComponent(entityId, current.get().withStatus(newStatus));
        log.info("Updated status: id={} -> {}", entityId, newStatus);
    }

    /**
     * Find an entity by name (linear scan — fine for <10k entities).
     */
    public Optional<EntityId> findByName(EntityStore store, String name) {
        return store.queryEntitiesWith(IdentityComponent.class).stream()
                .map(id -> store.getComponent(id, IdentityComponent.class).map(ic -> new java.util.AbstractMap.SimpleEntry<>(id, ic)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(e -> e.getValue().name().equals(name))
                .map(java.util.AbstractMap.SimpleEntry::getKey)
                .findFirst();
    }

    /**
     * List all active identities.
     */
    public List<EntityId> listActive(EntityStore store) {
        return store.queryEntitiesWith(IdentityComponent.class).stream()
                .filter(id -> store.getComponent(id, IdentityComponent.class)
                        .map(ic -> ic.status() == IdentityComponent.EntityStatus.ACTIVE)
                        .orElse(false))
                .sorted()
                .toList();
    }

    @Override
    public void execute(EntityStore store) {
        Set<EntityId> identities = store.queryEntitiesWith(IdentityComponent.class);
        long active = identities.stream()
                .filter(id -> store.getComponent(id, IdentityComponent.class)
                        .map(ic -> ic.status() == IdentityComponent.EntityStatus.ACTIVE)
                        .orElse(false))
                .count();
        long paused = identities.stream()
                .filter(id -> store.getComponent(id, IdentityComponent.class)
                        .map(ic -> ic.status() == IdentityComponent.EntityStatus.PAUSED)
                        .orElse(false))
                .count();
        long terminated = identities.stream()
                .filter(id -> store.getComponent(id, IdentityComponent.class)
                        .map(ic -> ic.status() == IdentityComponent.EntityStatus.TERMINATED)
                        .orElse(false))
                .count();
        log.info("IdentitySystem scan: total={} active={} paused={} terminated={}",
                identities.size(), active, paused, terminated);
    }
}
