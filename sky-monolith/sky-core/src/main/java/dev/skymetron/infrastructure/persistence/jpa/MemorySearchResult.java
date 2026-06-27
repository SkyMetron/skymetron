package dev.skymetron.infrastructure.persistence.jpa;

import java.util.UUID;

/**
 * Projection for similarity search results — maps the native query row
 * (entity columns + computed {@code similarity} column).
 */
public interface MemorySearchResult {

    UUID getId();
    String getContent();
    String getSource();
    Double getConfidence();
    String getMetadata();
    Double getSimilarity();
}
