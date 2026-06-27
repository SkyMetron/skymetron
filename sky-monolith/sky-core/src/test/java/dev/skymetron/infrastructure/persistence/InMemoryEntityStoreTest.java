package dev.skymetron.infrastructure.persistence;

import dev.skymetron.domain.execution.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryEntityStore tests")
class InMemoryEntityStoreTest {

    private InMemoryEntityStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryEntityStore();
    }

    @Test
    @DisplayName("createEntity() returns a unique id and the entity exists")
    void createEntity() {
        EntityId id = store.createEntity();

        assertThat(id).isNotNull();
        assertThat(store.exists(id)).isTrue();
        assertThat(store.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("attachComponent() and getComponent() round-trip")
    void attachAndGetComponent() {
        EntityId id = store.createEntity();
        IdentityComponent identity = new IdentityComponent("CEO", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE);

        store.attachComponent(id, identity);

        Optional<IdentityComponent> result = store.getComponent(id, IdentityComponent.class);
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("CEO");
        assertThat(store.hasComponent(id, IdentityComponent.class)).isTrue();
    }

    @Test
    @DisplayName("attachComponent() replaces existing component of same type")
    void replaceComponent() {
        EntityId id = store.createEntity();
        store.attachComponent(id, new IdentityComponent("A", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));
        store.attachComponent(id, new IdentityComponent("B", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.PAUSED));

        IdentityComponent result = store.getComponent(id, IdentityComponent.class).orElseThrow();
        assertThat(result.name()).isEqualTo("B");
        assertThat(result.status()).isEqualTo(IdentityComponent.EntityStatus.PAUSED);
    }

    @Test
    @DisplayName("detachComponent() removes a component")
    void detachComponent() {
        EntityId id = store.createEntity();
        store.attachComponent(id, new IdentityComponent("X", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));

        store.detachComponent(id, IdentityComponent.class);

        assertThat(store.hasComponent(id, IdentityComponent.class)).isFalse();
        assertThat(store.getComponent(id, IdentityComponent.class)).isEmpty();
    }

    @Test
    @DisplayName("getComponents() returns all attached components")
    void getAllComponents() {
        EntityId id = store.createEntity();
        store.attachComponent(id, new IdentityComponent("C", IdentityComponent.EntityType.MISSION, IdentityComponent.EntityStatus.ACTIVE));
        store.attachComponent(id, new CapabilityComponent(Set.of("read")));

        var components = store.getComponents(id);
        assertThat(components).hasSize(2);
    }

    @Test
    @DisplayName("queryEntitiesWith() returns only entities having the component type")
    void queryByComponentType() {
        EntityId a = store.createEntity();
        EntityId b = store.createEntity();
        EntityId c = store.createEntity();
        store.attachComponent(a, new IdentityComponent("A", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));
        store.attachComponent(b, new IdentityComponent("B", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));

        Set<EntityId> withIdentity = store.queryEntitiesWith(IdentityComponent.class);

        assertThat(withIdentity).containsExactlyInAnyOrder(a, b);
        assertThat(withIdentity).doesNotContain(c);
    }

    @Test
    @DisplayName("queryEntitiesWithAll() returns entities having all specified types")
    void queryWithAllTypes() {
        EntityId a = store.createEntity();
        EntityId b = store.createEntity();
        store.attachComponent(a, new IdentityComponent("A", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));
        store.attachComponent(a, new CapabilityComponent(Set.of("read")));
        store.attachComponent(b, new IdentityComponent("B", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));

        Set<EntityId> result = store.queryEntitiesWithAll(IdentityComponent.class, CapabilityComponent.class);

        assertThat(result).containsExactly(a);
    }

    @Test
    @DisplayName("queryEntitiesWithAll() with no types returns all entities")
    void queryWithNoTypes() {
        store.createEntity();
        store.createEntity();

        Set<EntityId> result = store.queryEntitiesWithAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("removeEntity() deletes entity and its components")
    void removeEntity() {
        EntityId id = store.createEntity();
        store.attachComponent(id, new IdentityComponent("R", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));

        store.removeEntity(id);

        assertThat(store.exists(id)).isFalse();
        assertThat(store.count()).isZero();
    }

    @Test
    @DisplayName("getComponent() on non-existent entity throws")
    void getComponentMissingEntity() {
        EntityId missing = EntityId.of(UUID.randomUUID());
        assertThatThrownBy(() -> store.getComponent(missing, IdentityComponent.class))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("attachComponent() on non-existent entity throws")
    void attachToMissingEntity() {
        EntityId missing = EntityId.of(UUID.randomUUID());
        assertThatThrownBy(() -> store.attachComponent(missing,
                new IdentityComponent("Z", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE)))
                .isInstanceOf(NoSuchElementException.class);
    }
}
