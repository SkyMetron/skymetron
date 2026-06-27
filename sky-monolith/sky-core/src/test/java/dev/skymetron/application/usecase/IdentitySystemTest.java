package dev.skymetron.application.usecase;

import dev.skymetron.domain.execution.*;
import dev.skymetron.infrastructure.persistence.InMemoryEntityStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IdentitySystem tests")
class IdentitySystemTest {

    private InMemoryEntityStore store;
    private IdentitySystem system;

    @BeforeEach
    void setUp() {
        store = new InMemoryEntityStore();
        system = new IdentitySystem();
    }

    @Test
    @DisplayName("createIdentity() attaches IdentityComponent and optional CapabilityComponent")
    void createIdentity() {
        EntityId id = system.createIdentity(store, "CEO",
                IdentityComponent.EntityType.AGENT,
                Set.of("memory.read", "tool.execute"));

        assertThat(store.hasComponent(id, IdentityComponent.class)).isTrue();
        assertThat(store.hasComponent(id, CapabilityComponent.class)).isTrue();

        IdentityComponent identity = store.getComponent(id, IdentityComponent.class).orElseThrow();
        assertThat(identity.name()).isEqualTo("CEO");
        assertThat(identity.status()).isEqualTo(IdentityComponent.EntityStatus.ACTIVE);

        CapabilityComponent caps = store.getComponent(id, CapabilityComponent.class).orElseThrow();
        assertThat(caps.has("memory.read")).isTrue();
    }

    @Test
    @DisplayName("createIdentity() without capabilities attaches only IdentityComponent")
    void createIdentityNoCaps() {
        EntityId id = system.createIdentity(store, "Worker",
                IdentityComponent.EntityType.AGENT, null);

        assertThat(store.hasComponent(id, IdentityComponent.class)).isTrue();
        assertThat(store.hasComponent(id, CapabilityComponent.class)).isFalse();
    }

    @Test
    @DisplayName("createIdentity() with empty capabilities set does not attach CapabilityComponent")
    void createIdentityEmptyCaps() {
        EntityId id = system.createIdentity(store, "Bare",
                IdentityComponent.EntityType.SESSION, Set.of());

        assertThat(store.hasComponent(id, CapabilityComponent.class)).isFalse();
    }

    @Test
    @DisplayName("updateStatus() transitions the IdentityComponent status")
    void updateStatus() {
        EntityId id = system.createIdentity(store, "Agent",
                IdentityComponent.EntityType.AGENT, Set.of());

        system.updateStatus(store, id, IdentityComponent.EntityStatus.PAUSED);

        IdentityComponent updated = store.getComponent(id, IdentityComponent.class).orElseThrow();
        assertThat(updated.status()).isEqualTo(IdentityComponent.EntityStatus.PAUSED);
    }

    @Test
    @DisplayName("updateStatus() throws for entity without IdentityComponent")
    void updateStatusNoIdentity() {
        EntityId id = store.createEntity();
        assertThatThrownBy(() -> system.updateStatus(store, id, IdentityComponent.EntityStatus.PAUSED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no IdentityComponent");
    }

    @Test
    @DisplayName("findByName() returns the entity with the matching name")
    void findByName() {
        system.createIdentity(store, "Alpha", IdentityComponent.EntityType.AGENT, Set.of());
        EntityId betaId = system.createIdentity(store, "Beta", IdentityComponent.EntityType.AGENT, Set.of());

        Optional<EntityId> found = system.findByName(store, "Beta");

        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(betaId);
    }

    @Test
    @DisplayName("findByName() returns empty when name not found")
    void findByNameNotFound() {
        system.createIdentity(store, "Alpha", IdentityComponent.EntityType.AGENT, Set.of());

        Optional<EntityId> found = system.findByName(store, "Nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("listActive() returns only ACTIVE entities")
    void listActive() {
        EntityId a = system.createIdentity(store, "A", IdentityComponent.EntityType.AGENT, Set.of());
        EntityId b = system.createIdentity(store, "B", IdentityComponent.EntityType.AGENT, Set.of());
        system.updateStatus(store, b, IdentityComponent.EntityStatus.PAUSED);

        List<EntityId> active = system.listActive(store);

        assertThat(active).containsExactly(a);
    }

    @Test
    @DisplayName("execute() runs without error and processes all identities")
    void executeRunsCleanly() {
        system.createIdentity(store, "A", IdentityComponent.EntityType.AGENT, Set.of());
        system.createIdentity(store, "B", IdentityComponent.EntityType.AGENT, Set.of());

        system.execute(store);
    }

    @Test
    @DisplayName("execute() handles empty store")
    void executeEmpty() {
        system.execute(store);
    }
}
