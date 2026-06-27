package dev.skymetron.application.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.skymetron.infrastructure.metrics.SkyMetricsRegistry;
import dev.skymetron.infrastructure.persistence.jpa.TraceEntry;
import dev.skymetron.infrastructure.persistence.jpa.TraceEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TraceAgent batch flushing")
class TraceAgentBatchTest {

    @Mock
    TraceEntryRepository repository;

    @Mock
    SkyMetricsRegistry metrics;

    ObjectMapper objectMapper = new ObjectMapper();

    TraceAgent agent;

    @BeforeEach
    void setUp() {
        agent = new TraceAgent(repository, objectMapper, metrics);
    }

    @Test
    @DisplayName("flushBatch() writes nothing when batch is empty")
    void flushEmptyBatch() {
        agent.flushBatch();
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("flushBatch() writes all pending events")
    void flushPendingBatch() {
        agent.onEvent("{\"eventType\":\"agent.invoked\",\"eventId\":\"a\",\"traceId\":\"b\",\"agentId\":\"c\"}");
        agent.onEvent("{\"eventType\":\"memory.stored\",\"eventId\":\"d\",\"traceId\":\"e\",\"agentId\":\"f\"}");

        agent.flushBatch();

        verify(repository).saveAll(argThat((java.util.List<TraceEntry> list) -> list.size() == 2));
    }
}
