package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.application.usecase.IdentitySystem;
import dev.skymetron.domain.execution.EntityId;
import dev.skymetron.domain.execution.EntityStore;
import dev.skymetron.domain.execution.IdentityComponent;
import dev.skymetron.infrastructure.audit.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/ecs")
@Tag(name = "ECS", description = "Entity Component System — identity management and component store")
public class EcsController {

    private final EntityStore entityStore;
    private final IdentitySystem identitySystem;

    public EcsController(EntityStore entityStore, IdentitySystem identitySystem) {
        this.entityStore = entityStore;
        this.identitySystem = identitySystem;
    }

    @AuditLog(action = "ecs.createIdentity", resource = "#request.name()")
    @Operation(summary = "Create an identity", description = "Creates a new entity with name, type, and capabilities")
    @PostMapping("/identities")
    public ResponseEntity<Map<String, Object>> createIdentity(@Valid @RequestBody CreateIdentityRequest request) {
        EntityId id = identitySystem.createIdentity(
                entityStore,
                request.name(),
                request.type(),
                request.capabilities());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", id.value().toString(), "name", request.name()));
    }

    @Operation(summary = "List active identities", description = "All registered entities with their type and status")
    @GetMapping("/identities")
    public ResponseEntity<List<IdentitySummary>> listActive() {
        List<EntityId> active = identitySystem.listActive(entityStore);
        List<IdentitySummary> result = active.stream()
                .map(id -> entityStore.getComponent(id, IdentityComponent.class)
                        .map(ic -> new IdentitySummary(id.value().toString(), ic.name(), ic.type().name(), ic.status().name())))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Count entities in ECS store")
    @GetMapping("/entities/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("total", entityStore.count()));
    }

    @Operation(summary = "Update identity status", description = "Change status (ACTIVE/PAUSED/TERMINATED) of an entity")
    @PostMapping("/identities/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String id,
                                              @RequestBody UpdateStatusRequest request) {
        identitySystem.updateStatus(entityStore,
                EntityId.of(id),
                IdentityComponent.EntityStatus.valueOf(request.status()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get entity components", description = "All components attached to a given entity")
    @GetMapping("/entities/{id}/components")
    public ResponseEntity<Map<String, Object>> getComponents(@PathVariable String id) {
        EntityId entityId = EntityId.of(id);
        if (!entityStore.exists(entityId)) {
            return ResponseEntity.notFound().build();
        }
        var components = entityStore.getComponents(entityId);
        return ResponseEntity.ok(Map.of("components", components));
    }

    @AuditLog(action = "ecs.removeEntity", resource = "#id")
    @Operation(summary = "Remove entity", description = "Permanently deletes an entity and all its components")
    @DeleteMapping("/entities/{id}")
    public ResponseEntity<Void> removeEntity(@PathVariable String id) {
        EntityId entityId = EntityId.of(id);
        if (entityStore.exists(entityId)) {
            entityStore.removeEntity(entityId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    public record CreateIdentityRequest(
            @NotBlank String name,
            @NotNull IdentityComponent.EntityType type,
            Set<String> capabilities
    ) {
    }

    public record UpdateStatusRequest(String status) {
    }

    public record IdentitySummary(String id, String name, String type, String status) {
    }
}
