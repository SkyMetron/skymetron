package dev.skymetron.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity backing the {@link dev.skymetron.infrastructure.persistence.JpaEntityStore}.
 *
 * <p>Components are stored as JSONB where each key is the component class
 * FQCN and the value is the serialized component record.
 */
@Entity
@Table(name = "ecs_entities")
public class EcsEntityRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "components", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> components;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EcsEntityRecord() {
    }

    public EcsEntityRecord(UUID id) {
        this.id = id;
        this.components = new java.util.HashMap<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Map<String, Object> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Object> components) {
        this.components = components;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
