# Retrospective — Sprints 6-8

**Data:** 2026-06-26

---

## Sprint 6 — Event Bus + Trace Agent

### What went well
- RabbitMQ topic exchange with `#` binding on trace queue works reliably
- 6 event types flowing: agent.invoked, memory.stored, memory.consolidated, tool.executed, research.completed, provider.fallback
- TraceAgent consumes all events and persists to `trace_entries` table
- EventPublisher enhanced JSON with `eventType` + `routingKey` fields

### Technical debt
- **Payload stored as stringified map** — `@JdbcTypeCode(SqlTypes.JSON)` stores the payload correctly but the display in the trace timeline shows Java `toString()` style (`@{key=value}`). Not a functional issue but affects readability in API responses.
- **No DLQ monitoring** — Dead letter queue is configured but no alert/monitoring on DLQ depth.
- **Testcontainers blocked** — Cannot run integration tests for RabbitMQ on this Windows setup (Docker Desktop pipe mismatch). Tests `*IT` isolated in `-Pintegration` profile.

---

## Sprint 7 — Brain View (Observabilidade Visual)

### What went well
- 23 custom `sky_*` metrics exposed via Micrometer + Prometheus
- Grafana dashboard with 12 panels (agents, providers, tokens, memory, loops, trace, errors, logs)
- Loki receiving Spring Boot logs via logback appender
- Prometheus scraping target up and scraping every 10s
- `autonomous=true` flag on all loop execution metrics
- Trace timeline API (`GET /api/trace/timeline`) working with pagination
- Trace agent search (`GET /api/trace/agent/{id}`) supporting both UUID and agent name via JSONB `@>` containment query

### Technical debt
- **Loki permission fix** — Loki 3.x container runs as non-root user. Had to add `user: "0"` to the docker-compose. Not ideal for production; should use proper volume permissions.
- **Memory count gauge updates infrequently** — `sky_memory_count` is set during consolidation loop (every 4h). For real-time dashboard, should update every health-check cycle (30s). Would require injecting MemoryService into LoopScheduler.
- **Trace agent/{id} JSONB query** — The native query `payload @> CAST(:jsonFilter AS jsonb)` may not scale well for large tables. Should add a separate `agent_name` column if performance becomes an issue.
- **No Grafana alert rules** — Dashboards are informative but no alerts configured for provider failures, high error rates, or loop failures.

---

## Sprint 8 — Knowledge Agent + QA Agent + Security Agent

### What went well
- 3 new agents implemented with full contracts (AgentRequest/AgentResponse)
- 31 new tests (149 total, 0 failures)
- KnowledgeAgent: skill catalog with add/get/update/confidence/delete/list commands
- QaAgent: test execution with pass/fail analysis, history, stats
- SecurityAgent: 7 vulnerability patterns (SQL Injection, Command Injection, Hardcoded Secrets, XSS, Path Traversal, Weak Crypto, Null Check Missing)
- All agents wired into CeoAgent routing (classifyIntent + selectTaskType)
- New domain events: `QaTestExecutedEvent`, `SecurityAnalysisEvent`
- New `Skill` record in `domain.knowledge` package

### Technical debt
- **In-memory state** — KnowledgeAgent stores skills in `ConcurrentHashMap`. No persistence across restarts. Should be backed by PostgreSQL in a future sprint.
- **QaAgent test execution is simulated** — Does not actually compile/run code, just parses `assert` statements. Real test execution would require spawning a JVM process (JUnit Platform Launcher).
- **SecurityAgent pattern matching is basic** — Simple string containment, no AST/static analysis. False positives on comments and string literals containing patterns (e.g., "use DES" matching weak crypto pattern).
- **CeoAgent classifyIntent is keyword-based** — Adding more intents makes the if-else chain harder to maintain. Should move to LLM-based intent classification when there are 10+ intents.
- **No integration tests for new agents** — Unit tests cover logic but no end-to-end test through CeoAgent → agent → response cycle with real wiring.

---

## Cross-sprint issues

### Persistent
1. **Testcontainers blocked** (Sprint 0) — Docker Desktop pipe mismatch on this Windows machine. All `*IT` tests must remain in `-Pintegration` profile.
2. **API keys in .env** (Sprint 0) — Works for dev but `.env` is gitignored and keys must be loaded manually. No encrypted secrets store yet.
3. **Only Groq provider tested live** — All 7 providers configured but only Groq has been exercised in live tests. Others may have integration issues not caught by unit tests.
4. **No desktop app** (Scheduled Sprint 11) — All interaction via REST API. No Electron frontend yet.

### Recommendations for Sprint 9
- Revisit KnowledgeAgent persistence (PostgreSQL-backed skill catalog)
- Add integration test profile for new agents
- Consider LLM-based intent classification in CeoAgent
- Set up Grafana alert rules for provider failures
- Add `sky_memory_count` gauge update in health-check loop

---

**Total tests:** 149  
**Sprint 7 DoD:** ✅ Complete  
**Sprint 8 DoD:** ✅ Complete
