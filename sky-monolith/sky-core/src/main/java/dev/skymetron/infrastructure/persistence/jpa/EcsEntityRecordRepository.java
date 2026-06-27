package dev.skymetron.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface EcsEntityRecordRepository extends JpaRepository<EcsEntityRecord, UUID> {

    /**
     * Check whether an entity has a given component type by querying the
     * JSONB keys. The component key is the fully-qualified class name.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM ecs_entities
                WHERE id = :id AND jsonb_exists(components, :componentKey)
            )
            """, nativeQuery = true)
    boolean hasComponent(UUID id, String componentKey);

    long count();
}
