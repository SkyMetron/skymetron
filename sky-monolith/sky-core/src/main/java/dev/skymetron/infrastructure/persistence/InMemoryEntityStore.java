package dev.skymetron.infrastructure.persistence;

import dev.skymetron.domain.execution.Component;
import dev.skymetron.domain.execution.EntityId;
import dev.skymetron.domain.execution.EntityStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory implementation of {@link EntityStore}.
 *
 * <p>Used for unit tests and local development. Component storage uses a
 * map keyed by entity id, with each entity's components keyed by their
 * concrete class. This allows O(1) lookup by type.
 */
public class InMemoryEntityStore implements EntityStore {

    private final Map<EntityId, Map<Class<?>, Component>> entities = new ConcurrentHashMap<>();

    @Override
    public EntityId createEntity() {
        EntityId id = EntityId.create();
        entities.put(id, new ConcurrentHashMap<>());
        return id;
    }

    @Override
    public void attachComponent(EntityId entityId, Component component) {
        requireExists(entityId);
        entities.get(entityId).put(component.getClass(), component);
    }

    @Override
    public void detachComponent(EntityId entityId, Class<? extends Component> componentType) {
        requireExists(entityId);
        entities.get(entityId).remove(componentType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(EntityId entityId, Class<T> componentType) {
        requireExists(entityId);
        return (Optional<T>) Optional.ofNullable(entities.get(entityId).get(componentType));
    }

    @Override
    public boolean hasComponent(EntityId entityId, Class<? extends Component> componentType) {
        requireExists(entityId);
        return entities.get(entityId).containsKey(componentType);
    }

    @Override
    public Collection<Component> getComponents(EntityId entityId) {
        requireExists(entityId);
        return Collections.unmodifiableCollection(entities.get(entityId).values());
    }

    @Override
    public Set<EntityId> queryEntitiesWith(Class<? extends Component> componentType) {
        Set<EntityId> result = new HashSet<>();
        for (var entry : entities.entrySet()) {
            if (entry.getValue().containsKey(componentType)) {
                result.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final Set<EntityId> queryEntitiesWithAll(Class<? extends Component>... componentTypes) {
        if (componentTypes.length == 0) {
            return Collections.unmodifiableSet(entities.keySet());
        }
        Set<EntityId> result = new HashSet<>();
        for (var entry : entities.entrySet()) {
            boolean hasAll = true;
            for (Class<? extends Component> type : componentTypes) {
                if (!entry.getValue().containsKey(type)) {
                    hasAll = false;
                    break;
                }
            }
            if (hasAll) {
                result.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public void removeEntity(EntityId entityId) {
        entities.remove(entityId);
    }

    @Override
    public boolean exists(EntityId entityId) {
        return entities.containsKey(entityId);
    }

    @Override
    public long count() {
        return entities.size();
    }

    private void requireExists(EntityId entityId) {
        if (!entities.containsKey(entityId)) {
            throw new NoSuchElementException("Entity not found: " + entityId);
        }
    }
}
