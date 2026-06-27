package dev.skymetron.domain.execution;

/**
 * Storage and retrieval for entities and their components.
 *
 * <p>Implementations may be in-memory (for tests/dev) or persisted
 * (e.g. JPA + JSONB for production).
 */
public interface EntityStore {

    /**
     * Create a new entity with a fresh id.
     */
    EntityId createEntity();

    /**
     * Attach (or replace) a component on an entity.
     */
    void attachComponent(EntityId entityId, Component component);

    /**
     * Remove a component type from an entity.
     */
    void detachComponent(EntityId entityId, Class<? extends Component> componentType);

    /**
     * Read a component of the given type from an entity.
     *
     * @return the component, or empty if not present
     */
    <T extends Component> java.util.Optional<T> getComponent(EntityId entityId, Class<T> componentType);

    /**
     * Check whether an entity has the given component type.
     */
    boolean hasComponent(EntityId entityId, Class<? extends Component> componentType);

    /**
     * Return all components attached to an entity.
     */
    java.util.Collection<Component> getComponents(EntityId entityId);

    /**
     * Return ids of all entities that have the given component type.
     */
    java.util.Set<EntityId> queryEntitiesWith(Class<? extends Component> componentType);

    /**
     * Return ids of all entities that have all the given component types.
     */
    java.util.Set<EntityId> queryEntitiesWithAll(Class<? extends Component>... componentTypes);

    /**
     * Remove an entity and all its components.
     */
    void removeEntity(EntityId entityId);

    /**
     * Whether the entity exists in this store.
     */
    boolean exists(EntityId entityId);

    /**
     * Total number of entities.
     */
    long count();
}
