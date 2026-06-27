package dev.skymetron.domain.memory;

import dev.skymetron.infrastructure.persistence.VectorUserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A single entry in the SkyMetron Vault — the unit of persistent memory.
 *
 * <p>Aggregate root of the Memory bounded context. Stores content, its
 * {@code nomic-embed-text} embedding (768 dims), metadata, source, confidence,
 * and a self-reference for deduplication ({@code mergedInto}).
 */
@Entity
@Table(name = "memory_entries")
public class MemoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Type(VectorUserType.class)
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "confidence", nullable = false)
    private double confidence = 1.0;

    @Column(name = "merged_into")
    private UUID mergedInto;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MemoryEntry() {
    }

    public MemoryEntry(String content, float[] embedding, MemoryType type, String source) {
        this.content = content;
        this.embedding = embedding;
        this.source = source;
        this.metadata.put("type", type.name().toLowerCase());
        this.confidence = 1.0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String content, float[] embedding) {
        this.content = content;
        this.embedding = embedding;
        this.updatedAt = Instant.now();
    }

    public void markMergedInto(UUID targetId) {
        this.mergedInto = targetId;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.mergedInto == null;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getSource() {
        return source;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
        this.updatedAt = Instant.now();
    }

    public UUID getMergedInto() {
        return mergedInto;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
