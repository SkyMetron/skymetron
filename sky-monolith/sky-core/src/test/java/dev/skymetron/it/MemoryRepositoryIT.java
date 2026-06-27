package dev.skymetron.it;

import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.infrastructure.ai.ollama.EmbeddingClient;
import dev.skymetron.infrastructure.persistence.jpa.MemoryEntryRepository;
import dev.skymetron.infrastructure.persistence.jpa.MemorySearchResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: verifies the full persistence stack — JPA entity mapping,
 * pgvector VectorConverter, HNSW similarity search — against a real
 * PostgreSQL 16 + pgvector container via Testcontainers.
 *
 * <p>Does NOT require Ollama or the Python AI service — embeddings are
 * generated locally as deterministic float arrays.
 *
 * <p>Run with: {@code mvn verify -Pintegration -pl sky-core}
 * (requires Docker on the host).
 */
@Tag("integration")
@SpringBootTest(classes = {dev.skymetron.SkyMetronApplication.class, TestContainersConfig.class})
@ActiveProfiles("test")
@Transactional
class MemoryRepositoryIT {

    @Autowired
    MemoryEntryRepository repository;

    @Test
    void saveAndRetrieveByEmbedding() {
        float[] vec = unitVector(768, 0);
        MemoryEntry entry = new MemoryEntry("SkyMetron is an AI operating system", vec, MemoryType.PROJECT_KNOWLEDGE, "test");
        repository.save(entry);

        assertThat(entry.getId()).isNotNull();
        assertThat(repository.findById(entry.getId()))
                .isPresent()
                .get()
                .satisfies(e -> assertThat(e.getEmbedding()).hasSize(768));
    }

    @Test
    void similaritySearchReturnsClosestFirst() {
        float[] vecA = unitVector(768, 0);
        float[] vecB = unitVector(768, 1);
        float[] queryVec = unitVector(768, 0);

        repository.save(new MemoryEntry("entry A — close to query", vecA, MemoryType.PROJECT_KNOWLEDGE, "test"));
        repository.save(new MemoryEntry("entry B — far from query", vecB, MemoryType.PROJECT_KNOWLEDGE, "test"));
        repository.flush();

        String literal = EmbeddingClient.toVectorLiteral(queryVec);
        List<MemorySearchResult> results = repository.searchBySimilarity(literal, null, 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getContent()).contains("entry A");
        assertThat(results.get(0).getSimilarity()).isGreaterThan(results.get(1).getSimilarity());
    }

    @Test
    void typeFilterNarrowsResults() {
        float[] vec = unitVector(768, 0);
        repository.save(new MemoryEntry("a fact", vec, MemoryType.USER_FACTS, "test"));
        repository.save(new MemoryEntry("a project note", vec, MemoryType.PROJECT_KNOWLEDGE, "test"));
        repository.flush();

        String literal = EmbeddingClient.toVectorLiteral(vec);
        List<MemorySearchResult> userFacts = repository.searchBySimilarity(literal, "user_facts", 10);

        assertThat(userFacts).hasSize(1);
        assertThat(userFacts.get(0).getContent()).isEqualTo("a fact");
    }

    @Test
    void duplicateDetectionFindsNearIdentical() {
        float[] vec = unitVector(768, 0);
        MemoryEntry original = repository.save(
                new MemoryEntry("original content", vec, MemoryType.PROJECT_KNOWLEDGE, "test"));
        repository.flush();

        String literal = EmbeddingClient.toVectorLiteral(vec);
        List<java.util.UUID> dupes = repository.findDuplicates(literal, 0.05, original.getId(), 5);

        assertThat(dupes).isEmpty();
    }

    private static float[] unitVector(int dims, int oneAt) {
        float[] v = new float[dims];
        v[oneAt % dims] = 1.0f;
        return v;
    }
}
