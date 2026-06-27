# PLANO DE ALINHAMENTO — SkyMetron v0.1.0-beta

**Data:** Junho 2026
**Versão:** v1.0
**Status:** Reunião de alinhamento — Vault + Situação do Projeto

---

## 1. ESTADO ATUAL DO PROJETO

### 1.1 Sprints Completas (0–15)

Roadmap completo do SKYMETRON PROTOCOL 0 — v4 executado integralmente:

| Sprint | Tema | Status |
|--------|------|--------|
| 0 | Fundação (Spring Boot 3, PostgreSQL 16, pgvector, Redis, RabbitMQ) | ✅ |
| 1 | Vault Integration (MemoryService, pgvector schema, embeddings) | ✅ |
| 2 | Core ECS (EntityComponentSystem, InMemory/JpaEntityStore) | ✅ |
| 3 | CEO Agent + Provider Registry (6 providers free + fallback) | ✅ |
| 4 | Memory Agent + Tool Agent (consulta vault, ferramentas) | ✅ |
| 5 | Research Agent + Deduplicação automática | ✅ |
| 6 | Event Bus + Trace Agent (RabbitMQ, Brain Trace) | ✅ |
| 7 | Observability Stack (Prometheus, Grafana, Loki) | ✅ |
| 8 | Quality + Security (Checkstyle, PMD, SpotBugs, JaCoCo) | ✅ |
| 9 | Motor Autônomo (OperationMode, LoopScheduler, 80/20 budget) | ✅ |
| 10 | Research Swarm (4 workers paralelos, Virtual Threads) | ✅ |
| 11 | Desktop App (Electron + React + TypeScript + Vite, 6 telas) | ✅ |
| 12 | Hardening + Security (JWT, RBAC, Rate Limit, Secrets) | ✅ |
| 13 | Performance + Optimization (Redis cache, HikariCP, batching) | ✅ |
| 14 | Update System + Deployment (CI/CD, auto-update, Docker Compose) | ✅ |
| 15 | Beta Release (OpenAPI/Swagger, CHANGELOG, release script) | ✅ |

### 1.2 O Que Foi Construído

| Componente | Quantidade | Detalhes |
|-----------|-----------|----------|
| **Agentes** | 18 classes | CeoAgent, MemoryAgent, ToolAgent, ResearchAgent, ConsolidationAgent, TraceAgent, KnowledgeAgent, QaAgent, SecurityAgent, ResearchSwarmAgent + 4 ResearchWorkers + 3 helpers |
| **Eventos de Domínio** | 12 classes | AgentInvokedEvent, MemoryStoredEvent, MemoryConsolidatedEvent, ToolExecutedEvent, ProviderFallbackEvent, ResearchCompletedEvent, SwarmCompletedEvent, SecurityAnalysisEvent, QaTestExecutedEvent, SystemOverloadEvent, SystemRecoveryEvent, SkyEvent |
| **LLM Providers** | 9 classes | Mistral, NVIDIA, Google Gemini, Groq, OpenRouter, OllamaLocal, Cerebras, OpenAiCompatible + ProviderRegistry |
| **REST Controllers** | 9 endpoints | Auth, Chat, Memory, Trace, ECS, Loop, Migration, AgentMetrics, Admin |
| **Telas Desktop** | 6 telas | Chat, Brain View, Memory, Loops, Providers, Settings |
| **Flyway Migrations** | 4 migrations | pgvector extension, memory_entries, ecs_entities, trace_entries |
| **Total Java** | 169 arquivos | Domain (52) + Application (34) + Infrastructure (52) + Testes (28) + Config/Resources |

### 1.3 Organização GitHub — 7 Repositórios

```
SkyMetron/
├── skymetron (PÚBLICO)              ← Core + Desktop + CI/CD
├── skymetron-ai-services (PRIVADO)  ← Python/FastAPI (extraído com histórico)
├── skymetron-docs (PÚBLICO)         ← ADRs + documentação
├── skymetron-deployment (PRIVADO)   ← Scripts + Docker Compose prod
├── skymetron-monitoring (PRIVADO)   ← Grafana + Prometheus + Loki
├── skymetron-vault (PRIVADO)        ← Placeholder (apenas README)
└── skymetron-secrets (PRIVADO)      ← Placeholder (apenas README)
```

### 1.4 Testes

| Tipo | Quantidade | Status |
|------|-----------|--------|
| Testes unitários Java | 24 classes de teste + 1 SmokeTest | ✅ `mvn test` passando |
| Testes de integração | 3 (*IT.java) | ⚠️ Isolados em `-Pintegration` |
| Testes Desktop E2E | 5 (Playwright) | ✅ Passando |
| **Total** | **202 testes, 0 falhas** | ✅ **BUILD SUCCESS** |

**Cobertura por camada:**
- Agent tests (10): CeoAgent, MemoryAgent, ToolAgent, ResearchAgent, ConsolidationAgent, TraceAgent(batch), KnowledgeAgent, QaAgent, SecurityAgent, ResearchSwarmAgent
- Use case tests (5): MemoryService(2), IdentitySystem, AgentSafetyPolicy, SessionBudget
- Infrastructure tests (7): JwtTokenProvider, RateLimitingFilter, SecretManager, AdminController, LoopController, InMemoryEntityStore, ProviderRegistry
- Domain tests (1): ComponentsTest
- Integration tests (3): ContextLoads, JpaEntityStore, MemoryRepository

---

## 2. SITUAÇÃO DO VAULT — PONTO CRÍTICO

### 2.1 Existem DOIS "Vaults" no Projeto

#### VAULT A — ENIAC VAULT (Legacy, com dados reais)

| Atributo | Valor |
|----------|-------|
| **Localização** | `SkyMetron/ENIAC_METRON/` |
| **Git próprio** | Sim — repo independente (NÃO está no git do SkyMetron) |
| **Último commit** | `e54cce8` — "docs(vault): record Sprint-008 RAG operational session" |
| **Conteúdo** | 700+ arquivos, ~22MB de conhecimento |
| **Notas Markdown** | 265 arquivos `.md` organizados em 25 categorias (00-Dashboard.md a 99-ARCHIVE) |
| **Obsidian vault** | ✅ Já configurado — `.obsidian/` com workspace, graph, appearance |
| **Status no plano** | "ARCHIVED / READ ONLY / PINNED" (Seção 1.1 do Protocolo) |
| **O que contém** | Memória persistente, fatos, sessões de chat, skills, regras, arquitetura, decisões de design |

**Caminho OBSIDIAN:**
```
C:\Users\USUARIO\Desktop\PROJETOS\SkyMetron\ENIAC_METRON\knowledge
```
(Abra como vault: File → Open Vault → Open folder as vault)

#### VAULT B — SKYMETRON VAULT (Novo, vazio de dados)

| Atributo | Valor |
|----------|-------|
| **Módulo Maven** | `sky-monolith/sky-vault/` — contém apenas `pom.xml + target/` |
| **Schema** | pgvector com 4 migrations Flyway (V1 a V4) em `sky-core` |
| **Código vault** | `MemoryEntry`, `MemoryService`, `MemoryAgent`, `MemoryController` em `sky-core` |
| **Banco** | PostgreSQL 16 com pgvector, tabela `memory_entries` com índice HNSW |
| **Dados atuais** | **VAZIO** — nunca foi populado |
| **Classes de migração** | `EniacVaultMigration.java`, `VaultScanner.java`, `VaultDocument.java`, `MigrationReport.java` em `application/migration/` |
| **Diretórios vazios** | `sky-vault/migration/` (vazio), `sky-vault/schema/` (vazio), `sky-vault/seeds/` (vazio) |

### 2.2 O Plano Original vs. Realidade

**SKYMETRON PROTOCOL (Seção 7.2) previa:**
> Migração ENIAC Vault — Fase 1 (Inventário), Fase 2 (Validação), Fase 3 (Transformação), Fase 4 (Importação)

**O que foi implementado:**
- Classes Java de migração escritas (`EniacVaultMigration.java`, `VaultScanner.java`)
- Schema pgvector criado (Flyway V1+V2)
- `MigrationController` expõe endpoints REST para importação
- ✅ **A MIGRAÇÃO REAL NUNCA FOI EXECUTADA** — nenhum dado do ENIAC vault foi importado para o PostgreSQL

**O que está faltando:**
| Item | Status | Detalhe |
|------|--------|---------|
| `sky-ai-services/embeddings/service.py` | ❌ Nunca existiu | O plano listava, mas a implementação fez o embedding via `EmbeddingClient.java` → `main.py` (FastAPI) → Ollama. O módulo `embeddings/` com `service.py` separado nunca foi criado. |
| `sky-vault/migration/` | 📂 Vazio | Sem scripts SQL, sem arquivos de migração de dados |
| `sky-vault/schema/` | 📂 Vazio | Schema está nas migrations Flyway em `sky-core` |
| `sky-vault/seeds/` | 📂 Vazio | Sem dados seed |
| Importação real do ENIAC | ❌ Não executada | Nenhum dos 700+ documentos foi vetorizado e salvo no pgvector |

---

## 3. INFRAESTRUTURA

### 3.1 Stack Tecnológico

| Camada | Tecnologia |
|--------|-----------|
| Linguagem Core | Java 21 LTS |
| Framework | Spring Boot 3.3.5 |
| Build | Maven |
| Frontend | React 18 + TypeScript + Vite 5 |
| Desktop | Electron 32 + electron-builder 25 |
| Banco Principal | PostgreSQL 16 + pgvector |
| Cache | Redis 7.4 |
| Event Bus | RabbitMQ 3.13 |
| AI Services | Python 3.12 + FastAPI |
| Embeddings | Ollama (nomic-embed-text, 768 dims) |
| LLM Providers | Mistral, NVIDIA, Google Gemini, Groq, OpenRouter, Ollama Local |
| Observability | Prometheus + Grafana + Loki |
| API Docs | Swagger UI (`/swagger-ui.html`) |
| CI/CD | GitHub Actions (CI + Release) |
| Container Registry | ghcr.io |
| Auto-update | electron-updater via GitHub Releases |

### 3.2 Docker Compose

`sky-monolith/docker-compose.yml` — serviços:

| Serviço | Imagem/Fonte | Porta |
|---------|-------------|-------|
| `postgres` | `pgvector/pgvector:pg16` | 5432 |
| `redis` | `redis:7.4-alpine` | 6379 |
| `rabbitmq` | `rabbitmq:3.13-management` | 5672 / 15672 |
| `ai-services` | `ghcr.io/skymetron/.../sky-ai-services:latest` | 8001 |
| `sky-core` | Build local (`Dockerfile.sky-core`) | 8080 |

### 3.3 GitHub Actions

| Workflow | Trigger | Faz |
|----------|---------|-----|
| `ci.yml` | Push/PR em main/master | Build Java + testes + Build Desktop (TS+Vite) |
| `release.yml` | Tag `v*.*.*` | Build JAR + Desktop (3 OS) + Docker → GitHub Release |

**Release v0.1.0-beta publicada com:**
- `sky-core-0.1.0-SNAPSHOT.jar`
- `SkyMetron-0.1.0-arm64.dmg` (macOS ARM64)
- `SkyMetron-0.1.0.AppImage` (Linux)
- ⚠️ Windows `.exe` — NÃO gerado (NSIS build sem artefato)
- ⚠️ Docker images — push bem-sucedido para `ghcr.io/skymetron/skymetron/`

---

## 4. PENDÊNCIAS DO PROTOCOLO 0

### 4.1 DoDs Não Cumpridos (Sprint 15)

- [ ] **Teste manual completo** — nunca executado:
  - Instalar do zero, configurar API keys, enviar mensagem, ver Brain Trace, ver memória, loops, fallback offline, auto-update
- [ ] **Spike de retrospectiva final** — faltam Sprints 9-15 (documentado até Sprint 8)

### 4.2 Definição de Sucesso (Seção 11) — 10/11 critérios

| Critério | Status | Obs |
|----------|--------|-----|
| Memória sobrevive a reinstalação | ✅ | pgvector + Flyway |
| Agentes substituíveis | ✅ | Agent interface |
| Providers trocáveis | ✅ | ProviderRegistry |
| 100% free | ✅ | 6 providers free |
| Offline (Ollama) | ✅ | Último na fallback |
| Conhecimento entre sessões | ✅ | pgvector persistente |
| Atualizações via GitHub Releases | ✅ | v0.1.0-beta |
| Controle total via desktop | ⚠️ | Telas existem, não testadas |
| 5+ agentes | ✅ | 10 implementados |
| Brain Trace | ✅ | Timeline + filtros |
| Zero dívida não documentada | ⚠️ | Faltam retros 9-15 |

### 4.3 Gaps Técnicos Conhecidos

- `embeddings/service.py` nunca foi criado (EmbeddingClient.java chama Ollama direto)
- `sky-vault/migration/`, `schema/`, `seeds/` — vazios
- Windows `.exe` não gerado
- macOS apenas ARM64
- Testcontainers não funciona no Windows local
- WebSocket não implementado (desktop usa polling REST)
- `skymetron-ai-services` sem CI próprio
- Docker image `sky-ai-services` sem release workflow independente

---

## 5. DECISÕES PARA A REUNIÃO

### 5.1 Destino do ENIAC Vault (700+ arquivos, ~22MB)

| Opção | Descrição | Esforço |
|-------|-----------|---------|
| **A** | Manter como está — consulta histórica via Obsidian em `knowledge/` | Nenhum |
| **B** | Mover `knowledge/` para dentro do SkyMetron como vault oficial | 1 dia |
| **C** | Migrar dados para pgvector via `EniacVaultMigration.java` | 3-5 dias |
| **D** | Criar vault Markdown em `docs/vault/` com link simbólico ao ENIAC | 1 dia |

### 5.2 Diretórios Vazios do sky-vault

- Preencher com scripts SQL / seeds?
- Remover (schema já está nas migrations Flyway)?
- Manter para uso futuro?

### 5.3 Repositórios Placeholder

- `skymetron-vault` (privado) — utilidade real ou remover?
- `skymetron-secrets` (privado) — utilidade real ou remover?

### 5.4 Próximo Passos

- Iniciar ciclo de correções beta → **v1.0.0**?
- Planejar **Protocol 1** (WebSocket, GraalVM, multi-instância)?
- Executar **teste manual** da Sprint 15?

---

## 6. CRONOGRAMA SUGERIDO

| Prioridade | Tarefa | Esforço |
|-----------|--------|---------|
| 🔴 Alta | Decidir destino do vault ENIAC | Decisão |
| 🔴 Alta | Executar teste manual (Sprint 15 DoD) | 1 dia |
| 🟡 Média | Preencher/remover pastas vazias sky-vault | 1 dia |
| 🟡 Média | Retrospectiva Sprints 9-15 | 2 dias |
| 🟡 Média | Consertar Windows .exe + macOS x64 | 2 dias |
| 🟢 Baixa | CI próprio skymetron-ai-services | 1 dia |
| 🟢 Baixa | Remover placeholders sem uso | 1 hora |

---

**In Veritas, Fundamentum.**
