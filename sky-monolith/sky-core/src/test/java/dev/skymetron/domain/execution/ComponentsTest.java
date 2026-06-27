package dev.skymetron.domain.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Component records tests")
class ComponentsTest {

    @Test
    @DisplayName("IdentityComponent validates non-blank name")
    void identityValidatesName() {
        assertThatThrownBy(() -> new IdentityComponent("", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdentityComponent(null, IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("IdentityComponent withStatus returns new instance with updated status")
    void identityWithStatus() {
        IdentityComponent original = new IdentityComponent("X", IdentityComponent.EntityType.AGENT, IdentityComponent.EntityStatus.ACTIVE);
        IdentityComponent updated = original.withStatus(IdentityComponent.EntityStatus.TERMINATED);

        assertThat(updated.status()).isEqualTo(IdentityComponent.EntityStatus.TERMINATED);
        assertThat(original.status()).isEqualTo(IdentityComponent.EntityStatus.ACTIVE);
    }

    @Test
    @DisplayName("MemoryComponent default constructor has empty set")
    void memoryDefaultEmpty() {
        MemoryComponent mc = new MemoryComponent();
        assertThat(mc.memoryRefs()).isEmpty();
    }

    @Test
    @DisplayName("MemoryComponent add returns new instance with added ref")
    void memoryAdd() {
        UUID id1 = UUID.randomUUID();
        MemoryComponent mc = new MemoryComponent().add(id1);

        assertThat(mc.memoryRefs()).containsExactly(id1);
    }

    @Test
    @DisplayName("MemoryComponent remove returns new instance without the ref")
    void memoryRemove() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        MemoryComponent mc = new MemoryComponent(Set.of(id1, id2)).remove(id1);

        assertThat(mc.memoryRefs()).containsExactly(id2);
    }

    @Test
    @DisplayName("MemoryComponent is immutable (defensive copy)")
    void memoryImmutable() {
        var source = new java.util.HashSet<UUID>();
        source.add(UUID.randomUUID());
        MemoryComponent mc = new MemoryComponent(source);
        source.clear();

        assertThat(mc.memoryRefs()).hasSize(1);
    }

    @Test
    @DisplayName("CapabilityComponent has/add work correctly")
    void capabilityOps() {
        CapabilityComponent cc = new CapabilityComponent();
        assertThat(cc.has("read")).isFalse();

        CapabilityComponent updated = cc.add("read");
        assertThat(updated.has("read")).isTrue();
        assertThat(cc.has("read")).isFalse();
    }

    @Test
    @DisplayName("EntityId create generates unique values")
    void entityIdCreateUnique() {
        EntityId a = EntityId.create();
        EntityId b = EntityId.create();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("EntityId rejects null")
    void entityIdRejectsNull() {
        assertThatThrownBy(() -> new EntityId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("EntityId of(String) parses UUID")
    void entityIdOfString() {
        String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
        EntityId id = EntityId.of(uuidStr);
        assertThat(id.value().toString()).isEqualTo(uuidStr);
    }
}
