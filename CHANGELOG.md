# Changelog

## v0.1.0-beta (2026-06-27)

Initial beta release of SkyMetron — an autonomous AI Operating System.

### Sprint 0 — Foundation

- Spring Boot 3.3.5 + Java 21 monolith
- PostgreSQL 16 with pgvector extension
- Redis 7 + RabbitMQ 3.13
- Flyway database migrations
- CORS configuration
- Dependency graph validated (no circular dependencies)

### Sprint 1 — Domain Core

- Domain layer: `AgentResponse`, `ExecutionContext`, `AgentMetrics`
- `Intent` classification enum
- `TaskType` for provider routing
- `EntityStore` interface for ECS persistence
- `ResourceBudget` for cost-aware operation
- `Message` and `LlmRequest/LlmResponse` for LLM communication

### Sprint 2 — ECS (Entity Component System)

- `EntityStore` in-memory implementation backed by `ConcurrentHashMap`
- `IdentityComponent` for entity metadata (name, type, status)
- `IdentitySystem` service for create/list/update/scan operations
- `EcsController` (REST: `POST/GET /api/ecs/identities`, `DELETE /api/ecs/entities/{id}`)
- 22 new entity/component tests

### Sprint 3 — Agent System

- 6 agents: `CeoAgent`, `MemoryAgent`, `ToolAgent`, `ResearchAgent`, `ConsolidationAgent`, `TraceAgent`
- `Agent` interface with `process()`, `capabilities()`, `health()`, `metrics()`
- `CeoAgent` — orchestrator with `classifyIntent()` routing
- `MemoryAgent` — vector memory store/query
- `ToolAgent` — filesystem tool execution
- `ResearchAgent` — web research with LLM summarization
- `ConsolidationAgent` — periodic Vault dedup (scheduled)
- `TraceAgent` — event-driven observability (RabbitMQ consumer)
- 67 tests total, `mvn clean verify` green

### Sprint 4 — Vault (Vector Memory)

- `MemoryEntry` domain entity with pgvector embedding
- `Vault` (in-memory + JPA `MemoryEntryRepository`)
- `MemoryService` — save, semantic search, list, delete, dedup
- Native SQL with `<=>` cosine distance via pgvector
- `MigrationController` — legacy Eniac vault import
- 84 total tests, BUILD SUCCESS

### Sprint 5 — Event Bus + Brain Trace

- RabbitMQ topic exchange with `TopicExchange` (durable)
- `EventPublisher` — publish events to trace topic
- `TraceAgent` — `@RabbitListener` consumes all events, persists `TraceEntry`
- `AgentInvokedEvent`, `MemoryStoredEvent`, `MemoryConsolidatedEvent`, `ToolExecutedEvent`, `ProviderFallbackEvent`
- `TraceEntryRepository` with JSONB payload querying
- `TraceController` — timeline, agent filter, event type filter, counts
- 100 total tests, BUILD SUCCESS

### Sprint 6 — Observability Stack

- Prometheus (`prometheus.yml`) with `sky-core` and `sky-ai-services` targets
- Loki (`loki-config.yml`) for log aggregation
- Grafana with auto-provisioned Prometheus + Loki datasources
- Micrometer `SkyMetricsRegistry` with 17 custom meters
- Docker Compose observability stack (`docker-compose.observability.yml`)
- 115 total tests, BUILD SUCCESS

### Sprint 7 — AI Services (Python/FastAPI)

- `sky-ai-services/` — Python FastAPI service for embeddings
- `POST /api/embeddings` — single text embedding
- `POST /api/embeddings/batch` — batch embedding
- `GET /health` — liveness check
- Ollama `nomic-embed-text` integration
- Dockerized with `sky-ai-services/Dockerfile`
- 118 total tests, BUILD SUCCESS

### Sprint 8 — Quality + Security

- `Checkstyle`, PMD, SpotBugs, JaCoCo (first run — baseline only)
- `SecurityAgent` — regex-based pattern scanner (credentials, SQL injection, secrets)
- `ToolAgent` — `SafetyPolicy` for path validation
- `RateLimitingFilter` — basic rate limiting
- 8 new security/deduplication tests
- 126 total tests, BUILD SUCCESS

### Sprint 9 — Motor Autônomo

- `OperationMode` (ACTIVE/IDLE/DEEP/SLEEP)
- `UserActivityTracker` — 30min idle → IDLE, 4h → SLEEP
- `LoopScheduler` — mode-aware, circuit breaker, budget 80/20
- `LoopRegistrar` — 3 loops (health-check 10s, consolidation 4h, research 1h)
- `LoopController` REST (status/pause/resume/mode)
- 26 new tests, 175 total, BUILD SUCCESS

### Sprint 10 — Research Swarm

- `ResearchWorker` interface + 4 workers (Web/Docs/Code/Paper)
- `ResearchSwarmAgent` — LLM decomposition, parallel execution (Virtual Threads), multi-source consolidation
- `SwarmCompletedEvent`, `TaskType` extensions
- 12 new tests, 187 total, BUILD SUCCESS

### Sprint 11 — Desktop App

- `sky-desktop/` — Electron 32 + React 18 + TypeScript + Vite 5
- 6 screens: Chat, Brain View, Memory, Loops, Providers, Settings
- API client centralized, HashRouter, dark theme
- Electron main/preload with context isolation
- 5 Playwright E2E tests passing
- Builds limpos (TypeScript + Vite + Electron)

### Sprint 12 — Hardening + Security

- Spring Security + JWT (jjwt 0.12.6), RBAC (admin/user/readonly)
- `RateLimitingFilter` (120 req/min/IP)
- `SecretManager` AES/GCM for API keys at rest
- `@AuditLog` aspect for sensitive operations
- `GlobalExceptionHandler`, `AuthController` (`POST /api/auth/login`)
- `@Valid`/`@NotBlank`/`@NotNull` on all DTOs
- 8 new security tests, 195 total, BUILD SUCCESS

### Sprint 13 — Performance + Optimization

- Redis caching (`@Cacheable`/`@CacheEvict`) for memory search and trace counts
- HikariCP tuning (connection-timeout, max-lifetime, leak-detection)
- Batch operations: `MemoryService.saveAll()`, `TraceAgent` event buffer (50 events / 5s flush)
- Lazy initialization (`@Lazy`) on all 8 providers + EmbeddingClient
- `AdminController` — benchmark (p95 latency), JVM memory, thread diagnostics
- New metrics: `sky.cache.hits/misses`, `sky.embeddings`, `sky.embedding.duration`
- 7 new tests, 202 total, BUILD SUCCESS

### Sprint 14 — Update System + Deployment

- GitHub Actions CI (Java/Python/Desktop) + Release workflow (tag → build → GitHub Release)
- `electron-updater` integration (auto-update via GitHub Releases)
- Production Docker Compose (`docker-compose.prod.yml`) with healthchecks, resource limits, `.env`
- Backup/restore scripts (`scripts/backup.sh`, `scripts/restore.sh`)
- `DEPLOYMENT.md` — full deployment guide + environment variables
- `UPGRADING.md` — version scheme, upgrade process, rollback strategy

### Sprint 15 — Beta Release

- OpenAPI docs (`springdoc-openapi`) — Swagger UI at `/swagger-ui.html`
- `@Tag`/`@Operation` annotations on all REST controllers (9 controllers, 30+ endpoints)
- `CHANGELOG.md` — complete sprint-by-sprint history
- Release automation scripts
- 202 tests, 0 failures, `mvn clean verify` BUILD SUCCESS
