package dev.skymetron.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TraceEntryRepository extends JpaRepository<TraceEntry, UUID> {

    Page<TraceEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<TraceEntry> findByAgentIdOrderByCreatedAtDesc(String agentId, Pageable pageable);

    @Query(value = """
            SELECT * FROM trace_entries
            WHERE payload @> CAST(:jsonFilter AS jsonb)
            ORDER BY created_at DESC
            """, countQuery = """
            SELECT count(*) FROM trace_entries
            WHERE payload @> CAST(:jsonFilter AS jsonb)
            """, nativeQuery = true)
    Page<TraceEntry> findByPayloadJsonb(@Param("jsonFilter") String jsonFilter, Pageable pageable);

    Page<TraceEntry> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);

    Page<TraceEntry> findByTraceIdOrderByCreatedAtDesc(UUID traceId, Pageable pageable);

    long countByEventType(String eventType);
}
