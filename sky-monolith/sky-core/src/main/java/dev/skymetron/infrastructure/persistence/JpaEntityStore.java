package dev.skymetron.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.skymetron.domain.execution.Component;
import dev.skymetron.domain.execution.EntityId;
import dev.skymetron.domain.execution.EntityStore;
import dev.skymetron.infrastructure.persistence.jpa.EcsEntityRecord;
import dev.skymetron.infrastructure.persistence.jpa.EcsEntityRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA-backed {@link EntityStore} that persists entities and their components
 * to PostgreSQL {@code ecs_entities} (JSONB).
 *
 * <p>Components are serialized to JSONB keyed by their fully-qualified class
 * name. On read, they are deserialized back to the concrete record type via
 * Jackson's default typing.
 */
@org.springframework.stereotype.Component
@Transactional
public class JpaEntityStore implements EntityStore {

    private static final Logger log = LoggerFactory.getLogger(JpaEntityStore.class);

    private final EcsEntityRecordRepository repository;
    private final ObjectMapper objectMapper;

    public JpaEntityStore(EcsEntityRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper.copy();
        this.objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
    }

    @Override
    public EntityId createEntity() {
        EntityId id = EntityId.create();
        repository.save(new EcsEntityRecord(id.value()));
        log.debug("Created entity {}", id);
        return id;
    }

    @Override
    public void attachComponent(EntityId entityId, Component component) {
        EcsEntityRecord record = requireRecord(entityId);
        Map<String, Object> comps = record.getComponents();
        comps.put(component.getClass().getName(), component);
        record.setComponents(comps);
        repository.save(record);
    }

    @Override
    public void detachComponent(EntityId entityId, Class<? extends Component> componentType) {
        EcsEntityRecord record = requireRecord(entityId);
        record.getComponents().remove(componentType.getName());
        repository.save(record);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(EntityId entityId, Class<T> componentType) {
        return requireRecord(entityId).getComponents().entrySet().stream()
                .filter(e -> e.getKey().equals(componentType.getName()))
                .map(e -> convertValue(e.getValue(), componentType))
                .map(v -> (T) v)
                .findFirst();
    }

    @Override
    public boolean hasComponent(EntityId entityId, Class<? extends Component> componentType) {
        return repository.hasComponent(entityId.value(), componentType.getName());
    }

    @Override
    public Collection<Component> getComponents(EntityId entityId) {
        EcsEntityRecord record = requireRecord(entityId);
        List<Component> result = new ArrayList<>();
        for (var entry : record.getComponents().entrySet()) {
            try {
                Class<?> clazz = Class.forName(entry.getKey());
                Object comp = convertValue(entry.getValue(), clazz);
                if (comp instanceof Component c) {
                    result.add(c);
                }
            } catch (ClassNotFoundException e) {
                log.warn("Unknown component class on entity {}: {}", entityId, entry.getKey());
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Set<EntityId> queryEntitiesWith(Class<? extends Component> componentType) {
        String key = componentType.getName();
        return repository.findAll().stream()
                .filter(r -> r.getComponents().containsKey(key))
                .map(r -> EntityId.of(r.getId()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final Set<EntityId> queryEntitiesWithAll(Class<? extends Component>... componentTypes) {
        Set<String> keys = Arrays.stream(componentTypes)
                .map(Class::getName)
                .collect(Collectors.toSet());
        return repository.findAll().stream()
                .filter(r -> r.getComponents().keySet().containsAll(keys))
                .map(r -> EntityId.of(r.getId()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void removeEntity(EntityId entityId) {
        repository.deleteById(entityId.value());
    }

    @Override
    public boolean exists(EntityId entityId) {
        return repository.existsById(entityId.value());
    }

    @Override
    public long count() {
        return repository.count();
    }

    private EcsEntityRecord requireRecord(EntityId entityId) {
        return repository.findById(entityId.value())
                .orElseThrow(() -> new NoSuchElementException("Entity not found: " + entityId));
    }

    private <T> T convertValue(Object source, Class<T> targetClass) {
        return objectMapper.convertValue(source, targetClass);
    }
}
