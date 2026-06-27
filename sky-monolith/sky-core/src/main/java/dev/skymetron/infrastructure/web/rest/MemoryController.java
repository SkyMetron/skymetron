package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.usecase.MemoryService;
import dev.skymetron.domain.memory.MemoryEntry;
import dev.skymetron.domain.memory.MemoryType;
import dev.skymetron.infrastructure.audit.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memory")
@Tag(name = "Memory", description = "Vault — semantic memory storage and search with pgvector")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @AuditLog(action = "memory.create", resource = "#request.source()")
    @Operation(summary = "Store a memory entry", description = "Embeds content with nomic-embed-text and persists to vector vault")
    @PostMapping
    public ResponseEntity<MemoryResponse> create(@Valid @RequestBody CreateMemoryRequest request) {
        MemoryEntry entry = memoryService.save(request.content(), request.type(), request.source());
        if (request.metadata() != null) {
            request.metadata().forEach(entry::addMetadata);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(MemoryResponse.from(entry));
    }

    @Operation(summary = "Search memory", description = "Semantic search via pgvector cosine distance")
    @GetMapping("/search")
    public ResponseEntity<List<SearchHitResponse>> search(@Parameter(description = "Query text") @RequestParam String q,
                                                          @RequestParam(required = false) MemoryType type,
                                                          @RequestParam(required = false, defaultValue = "10") int limit) {
        List<MemoryService.SearchHit> hits = memoryService.search(q, type, Math.min(limit, 100));
        List<SearchHitResponse> body = hits.stream()
                .map(h -> new SearchHitResponse(h.id(), h.content(), h.source(), h.confidence(), h.similarity()))
                .toList();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Get memory entry by ID")
    @GetMapping("/{id}")
    public ResponseEntity<MemoryResponse> getById(@Parameter(description = "Entry UUID") @PathVariable java.util.UUID id) {
        return memoryService.getById(id)
                .map(e -> ResponseEntity.ok(MemoryResponse.from(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List active memory entries", description = "Paginated listing, optionally filtered by type")
    @GetMapping
    public ResponseEntity<Page<MemoryResponse>> list(
            @RequestParam(required = false) MemoryType type,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<MemoryEntry> entries = memoryService.listActive(type, pageable);
        return ResponseEntity.ok(entries.map(MemoryResponse::from));
    }

    @AuditLog(action = "memory.delete", resource = "#id")
    @Operation(summary = "Delete memory entry")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Entry UUID") @PathVariable java.util.UUID id) {
        return memoryService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Count active memory entries")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("active", memoryService.countActive()));
    }
}
