package dev.skymetron.application.usecase;

import dev.skymetron.application.port.out.EmbeddingPort;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.infrastructure.persistence.jpa.MemoryEntryRepository;
import dev.skymetron.infrastructure.persistence.jpa.MemorySearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoryService unit tests")
class MemoryServiceTest {

    @Mock
    MemoryEntryRepository repository;

    @Mock
    EmbeddingPort embeddingPort;

    @InjectMocks
    MemoryService service;

    private static final float[] EMBEDDING = new float[]{0.1f, 0.2f, 0.3f};

    @Test
    @DisplayName("save() embeds content, persists, and returns entry")
    void saveEmbedsAndPersists() {
        when(embeddingPort.embed("hello")).thenReturn(EMBEDDING);
        when(repository.findDuplicates(anyString(), anyDouble(), any(), anyInt())).thenReturn(List.of());

        MemoryEntry result = service.save("hello", MemoryType.USER_FACTS, "test");

        assertThat(result.getContent()).isEqualTo("hello");
        assertThat(result.getEmbedding()).isEqualTo(EMBEDDING);
        assertThat(result.getMetadata()).containsEntry("type", "user_facts");
        verify(repository).save(any(MemoryEntry.class));
    }

    @Test
    @DisplayName("save() marks entry as merged when a duplicate is found")
    void saveMarksDuplicate() {
        when(embeddingPort.embed("dup")).thenReturn(EMBEDDING);
        UUID existingId = UUID.randomUUID();
        when(repository.findDuplicates(anyString(), anyDouble(), any(), anyInt()))
                .thenReturn(List.of(existingId));

        MemoryEntry result = service.save("dup", MemoryType.PROJECT_KNOWLEDGE, "test");

        assertThat(result.getMergedInto()).isEqualTo(existingId);
        verify(repository).save(any(MemoryEntry.class));
    }

    @Test
    @DisplayName("saveWithEmbedding() uses pre-computed embedding and does not call embeddingPort")
    void saveWithEmbeddingSkipsEmbedding() {
        when(repository.findDuplicates(anyString(), anyDouble(), any(), anyInt())).thenReturn(List.of());

        MemoryEntry result = service.saveWithEmbedding(
                "content", EMBEDDING, MemoryType.SESSION_CONTEXT, "migration", 0.8);

        assertThat(result.getConfidence()).isEqualTo(0.8);
        assertThat(result.getSource()).isEqualTo("migration");
        verify(embeddingPort, never()).embed(anyString());
        verify(repository).save(any(MemoryEntry.class));
    }

    @Test
    @DisplayName("search() embeds query and maps results to SearchHit")
    void searchEmbedsAndMaps() {
        when(embeddingPort.embed("query")).thenReturn(EMBEDDING);
        UUID hitId = UUID.randomUUID();
        MemorySearchResult mockResult = mock(MemorySearchResult.class);
        when(mockResult.getId()).thenReturn(hitId);
        when(mockResult.getContent()).thenReturn("hit content");
        when(mockResult.getSource()).thenReturn("src");
        when(mockResult.getConfidence()).thenReturn(0.9);
        when(mockResult.getSimilarity()).thenReturn(0.95);
        when(repository.searchBySimilarity(anyString(), any(), eq(10)))
                .thenReturn(List.of(mockResult));

        List<MemoryService.SearchHit> hits = service.search("query", null, 10);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).id()).isEqualTo(hitId);
        assertThat(hits.get(0).similarity()).isEqualTo(0.95);
        verify(embeddingPort).embed("query");
    }

    @Test
    @DisplayName("search() passes type filter as lowercase string")
    void searchPassesTypeFilter() {
        when(embeddingPort.embed("q")).thenReturn(EMBEDDING);
        when(repository.searchBySimilarity(anyString(), eq("user_facts"), anyInt()))
                .thenReturn(List.of());

        service.search("q", MemoryType.USER_FACTS, 5);

        verify(repository).searchBySimilarity(anyString(), eq("user_facts"), eq(5));
    }

    @Test
    @DisplayName("getById() delegates to repository")
    void getById() {
        UUID id = UUID.randomUUID();
        MemoryEntry entry = new MemoryEntry("content", EMBEDDING, MemoryType.USER_FACTS, "s");
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        Optional<MemoryEntry> result = service.getById(id);

        assertThat(result).isPresent();
        verify(repository).findById(id);
    }

    @Test
    @DisplayName("listActive() with no filter returns all active entries")
    void listActiveAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<MemoryEntry> page = new PageImpl<>(List.of());
        when(repository.findByMergedIntoIsNull(pageable)).thenReturn(page);

        Page<MemoryEntry> result = service.listActive(null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(repository).findByMergedIntoIsNull(pageable);
    }

    @Test
    @DisplayName("listActive() with type filter delegates to typed query")
    void listActiveWithType() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<MemoryEntry> page = new PageImpl<>(List.of());
        when(repository.findByMergedIntoIsNullAndMetadataType("project_knowledge", pageable))
                .thenReturn(page);

        service.listActive(MemoryType.PROJECT_KNOWLEDGE, pageable);

        verify(repository).findByMergedIntoIsNullAndMetadataType("project_knowledge", pageable);
    }

    @Test
    @DisplayName("delete() returns true when entry exists")
    void deleteExisting() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        boolean result = service.delete(id);

        assertThat(result).isTrue();
        verify(repository).deleteById(id);
    }

    @Test
    @DisplayName("delete() returns false when entry does not exist")
    void deleteMissing() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        boolean result = service.delete(id);

        assertThat(result).isFalse();
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("countActive() delegates to repository")
    void countActive() {
        when(repository.countByMergedIntoIsNull()).thenReturn(42L);

        long count = service.countActive();

        assertThat(count).isEqualTo(42L);
    }

    @Test
    @DisplayName("markMerged() marks the duplicate entry as merged into target")
    void markMerged() {
        UUID dupId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        MemoryEntry entry = new MemoryEntry("dup", EMBEDDING, MemoryType.USER_FACTS, "s");
        when(repository.findById(dupId)).thenReturn(Optional.of(entry));

        service.markMerged(dupId, targetId);

        assertThat(entry.getMergedInto()).isEqualTo(targetId);
    }

    @Test
    @DisplayName("markMerged() does nothing when duplicate not found")
    void markMergedNotFound() {
        UUID dupId = UUID.randomUUID();
        when(repository.findById(dupId)).thenReturn(Optional.empty());

        service.markMerged(dupId, UUID.randomUUID());

        verify(repository, never()).save(any());
    }
}
