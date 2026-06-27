package dev.skymetron.infrastructure.persistence.jpa;

import dev.skymetron.domain.memory.MemoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MemoryEntryRepository extends JpaRepository<MemoryEntry, UUID> {

    /**
     * Active (non-merged) entries only.
     */
    Page<MemoryEntry> findByMergedIntoIsNull(Pageable pageable);

    @Query(value = """
            SELECT * FROM memory_entries
            WHERE merged_into IS NULL AND metadata->>'type' = :type
            """,
            countQuery = """
            SELECT count(*) FROM memory_entries
            WHERE merged_into IS NULL AND metadata->>'type' = :type
            """,
            nativeQuery = true)
    Page<MemoryEntry> findByMergedIntoIsNullAndMetadataType(@Param("type") String type, Pageable pageable);

    /**
     * Semantic similarity search via pgvector cosine distance ({@code <=>}).
     *
     * <p>The query vector is passed as a pgvector literal string and cast in SQL.
     * Returns active entries ordered by ascending distance (most similar first).
     *
     * @param queryVectorLiteral pgvector-formatted string, e.g. {@code "[0.1,0.2,...]"}
     * @param type               optional metadata type filter (nullable for all)
     * @param limit              max results
     * @return matching entries with computed similarity
     */
    @Query(value = """
            SELECT *, 1 - (embedding <=> cast(:queryVector as vector)) AS similarity
            FROM memory_entries
            WHERE merged_into IS NULL
              AND (:type IS NULL OR metadata->>'type' = :type)
            ORDER BY embedding <=> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<MemorySearchResult> searchBySimilarity(
            @Param("queryVector") String queryVectorLiteral,
            @Param("type") String type,
            @Param("limit") int limit);

    /**
     * Finds candidate duplicates: active entries whose embedding is within
     * {@code threshold} cosine distance of the given vector.
     */
    @Query(value = """
            SELECT id
            FROM memory_entries
            WHERE merged_into IS NULL
              AND embedding <=> cast(:queryVector as vector) < :threshold
              AND id <> cast(:excludeId as uuid)
            ORDER BY embedding <=> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findDuplicates(
            @Param("queryVector") String queryVectorLiteral,
            @Param("threshold") double threshold,
            @Param("excludeId") UUID excludeId,
            @Param("limit") int limit);

    long countByMergedIntoIsNull();
}
