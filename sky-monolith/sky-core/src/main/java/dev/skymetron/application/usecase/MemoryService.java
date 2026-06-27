package dev.skymetron.application.usecase;

import dev.skymetron.application.port.out.EmbeddingPort;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.infrastructure.ai.ollama.EmbeddingClient;
import dev.skymetron.infrastructure.persistence.jpa.MemoryEntryRepository;
import dev.skymetron.infrastructure.persistence.jpa.MemorySearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
    private static final double DEDUP_THRESHOLD = 0.05;

    private final MemoryEntryRepository repository;
    private final EmbeddingPort embeddingPort;

    public MemoryService(MemoryEntryRepository repository, EmbeddingPort embeddingPort) {
        this.repository = repository;
        this.embeddingPort = embeddingPort;
    }

    @CacheEvict(value = "memorySearch", allEntries = true)
    public MemoryEntry save(String content, MemoryType type, String source) {
        float[] embedding = embeddingPort.embed(content);
        MemoryEntry entry = new MemoryEntry(content, embedding, type, source);
        repository.save(entry);
        log.debug("Saved memory entry id={} type={} source={}", entry.getId(), type, source);

        detectAndMarkDuplicate(entry, embedding);
        return entry;
    }

    @CacheEvict(value = "memorySearch", allEntries = true)
    public MemoryEntry saveWithEmbedding(String content, float[] embedding, MemoryType type,
                                         String source, double confidence) {
        MemoryEntry entry = new MemoryEntry(content, embedding, type, source);
        entry.setConfidence(confidence);
        repository.save(entry);
        detectAndMarkDuplicate(entry, embedding);
        return entry;
    }

    @CacheEvict(value = "memorySearch", allEntries = true)
    public List<MemoryEntry> saveAll(List<MemoryEntry> entries) {
        List<MemoryEntry> saved = repository.saveAll(entries);
        log.debug("Batch saved {} memory entries", saved.size());
        return saved;
    }

    private void detectAndMarkDuplicate(MemoryEntry entry, float[] embedding) {
        String literal = EmbeddingClient.toVectorLiteral(embedding);
        List<UUID> dupes = repository.findDuplicates(literal, DEDUP_THRESHOLD, entry.getId(), 1);
        if (!dupes.isEmpty()) {
            entry.markMergedInto(dupes.get(0));
            log.info("Memory entry {} marked as duplicate of {}", entry.getId(), dupes.get(0));
        }
    }

    @Cacheable(value = "memorySearch", key = "#queryText + ':' + #typeFilter?.name() + ':' + #limit")
    public List<SearchHit> search(String queryText, MemoryType typeFilter, int limit) {
        float[] queryEmbedding = embeddingPort.embed(queryText);
        return searchByVector(queryEmbedding, typeFilter, limit);
    }

    public List<SearchHit> searchByVector(float[] queryEmbedding, MemoryType typeFilter, int limit) {
        String literal = EmbeddingClient.toVectorLiteral(queryEmbedding);
        String type = typeFilter != null ? typeFilter.name().toLowerCase() : null;
        List<MemorySearchResult> results = repository.searchBySimilarity(literal, type, limit);
        return results.stream()
                .map(r -> new SearchHit(
                        r.getId(),
                        r.getContent(),
                        r.getSource(),
                        r.getConfidence(),
                        r.getSimilarity()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MemoryEntry> getById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<MemoryEntry> listActive(MemoryType typeFilter, Pageable pageable) {
        if (typeFilter != null) {
            return repository.findByMergedIntoIsNullAndMetadataType(
                    typeFilter.name().toLowerCase(), pageable);
        }
        return repository.findByMergedIntoIsNull(pageable);
    }

    @CacheEvict(value = "memorySearch", allEntries = true)
    public boolean delete(UUID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            log.debug("Deleted memory entry id={}", id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return repository.countByMergedIntoIsNull();
    }

    @Transactional(readOnly = true)
    public List<UUID> findDuplicatesRaw(String queryVectorLiteral, double threshold, UUID excludeId, int limit) {
        return repository.findDuplicates(queryVectorLiteral, threshold, excludeId, limit);
    }

    @CacheEvict(value = "memorySearch", allEntries = true)
    public void markMerged(UUID duplicateId, UUID targetId) {
        repository.findById(duplicateId).ifPresent(entry -> {
            entry.markMergedInto(targetId);
            log.info("Merged entry {} into {}", duplicateId, targetId);
        });
    }

    public record SearchHit(
            UUID id,
            String content,
            String source,
            double confidence,
            double similarity
    ) {
    }
}
