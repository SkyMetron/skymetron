package dev.skymetron.application.usecase;

import dev.skymetron.application.port.out.EmbeddingPort;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.infrastructure.persistence.jpa.MemoryEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoryService batch operations")
class MemoryServiceBatchTest {

    @Mock
    MemoryEntryRepository repository;

    @Mock
    EmbeddingPort embeddingPort;

    @InjectMocks
    MemoryService service;

    private static final float[] EMBEDDING = new float[]{0.1f, 0.2f, 0.3f};

    @Test
    @DisplayName("saveAll() delegates to repository.saveAll() and returns saved entries")
    void saveAllPersistsBatch() {
        MemoryEntry entry1 = new MemoryEntry("content1", EMBEDDING, MemoryType.USER_FACTS, "test");
        MemoryEntry entry2 = new MemoryEntry("content2", EMBEDDING, MemoryType.PROJECT_KNOWLEDGE, "test");
        List<MemoryEntry> input = List.of(entry1, entry2);

        when(repository.saveAll(input)).thenReturn(input);

        List<MemoryEntry> result = service.saveAll(input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(1).getContent()).isEqualTo("content2");
        verify(repository).saveAll(input);
    }
}
