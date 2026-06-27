package dev.skymetron.it;

import dev.skymetron.domain.execution.*;
import dev.skymetron.infrastructure.persistence.JpaEntityStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link JpaEntityStore} against a real PostgreSQL
 * container via Testcontainers.
 *
 * <p>Verifies that components are correctly serialized to JSONB and
 * deserialized back to their concrete record types.
 *
 * <p>Run with: {@code mvn verify -Pintegration -pl sky-core}
 */
@Tag("integration")
@SpringBootTest(classes = {dev.skymetron.SkyMetronApplication.class, TestContainersConfig.class})
@ActiveProfiles("test")
@Transactional
class JpaEntityStoreIT {

    @Autowired
    JpaEntityStore store;

    @Test
    void createAndAttachAndRetrieve() {
        EntityId id = store.createEntity();
        store.attachComponent(id, new IdentityComponent("TestAgent",
                IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));

        Optional<IdentityComponent> retrieved = store.getComponent(id, IdentityComponent.class);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().name()).isEqualTo("TestAgent");
        assertThat(retrieved.get().type()).isEqualTo(IdentityComponent.EntityType.AGENT);
    }

    @Test
    void queryEntitiesWithReturnsMatchingIds() {
        EntityId a = store.createEntity();
        EntityId b = store.createEntity();
        store.attachComponent(a, new IdentityComponent("A",
                IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE));

        Set<EntityId> withIdentity = store.queryEntitiesWith(IdentityComponent.class);

        assertThat(withIdentity).contains(a);
        assertThat(withIdentity).doesNotContain(b);
    }

    @Test
    void detachComponentRemovesIt() {
        EntityId id = store.createEntity();
        store.attachComponent(id, new CapabilityComponent(Set.of("read", "write")));

        store.detachComponent(id, CapabilityComponent.class);

        assertThat(store.hasComponent(id, CapabilityComponent.class)).isFalse();
    }

    @Test
    void multipleComponentsCoexist() {
        EntityId id = store.createEntity();
        store.attachComponent(id, new IdentityComponent("Multi",
                IdentityComponent.EntityType.MISSION, IdentityComponent.EntityStatus.ACTIVE));
        store.attachComponent(id, new CapabilityComponent(Set.of("search")));
        store.attachComponent(id, new MemoryComponent(Set.of(java.util.UUID.randomUUID())));

        var components = store.getComponents(id);
        assertThat(components).hasSize(3);
        assertThat(store.hasComponent(id, IdentityComponent.class)).isTrue();
        assertThat(store.hasComponent(id, CapabilityComponent.class)).isTrue();
        assertThat(store.hasComponent(id, MemoryComponent.class)).isTrue();
    }

    @Test
    void removeEntityDeletesRecord() {
        EntityId id = store.createEntity();
        store.attachComponent(id, new IdentityComponent("Doomed",
                IdentityComponent.EntityType.SESSION, IdentityComponent.EntityStatus.ACTIVE));

        store.removeEntity(id);

        assertThat(store.exists(id)).isFalse();
    }
}
