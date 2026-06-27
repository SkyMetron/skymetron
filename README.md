# SkyMetron

> AI Operating System — permanent memory, specialized agents, controlled autonomy,
> multi-provider LLM **100% free** (R$ 0,00/mês).

**Status:** Sprint 0 — Foundation.

---

## Repositórios / Módulos

| Módulo | Stack | Descrição |
|--------|-------|-----------|
| `sky-monolith/` | Java 21 + Spring Boot 3 + Maven | Monolito modular: `sky-core`, `sky-vault`, `sky-ai-bridge` |
| `sky-ai-services/` | Python 3.12 + FastAPI | Microservice de IA: embeddings (Ollama `nomic-embed-text`), OCR, NLP |
| `sky-docs/` | Markdown | Arquitetura, ADRs, planos |
| `ENIAC_METRON/` | _(arquivado, read-only)_ | Legacy — mantido apenas para migração do Vault |

---

## Pré-requisitos

| Ferramenta | Versão mínima | Verificar |
|-----------|---------------|-----------|
| Java (JDK) | 21 LTS | `java -version` |
| Maven | 3.9 | `mvn -version` |
| Docker + Compose | 24+ / v2 | `docker compose version` |
| Ollama | 0.1+ | `ollama --version` |
| Python | 3.11+ | `py --version` |

## Setup — passo a passo

### 1. Ollama + modelo de embeddings

O Ollama roda no **host** (não no Docker) para acesso à GPU/CPU local.

```powershell
ollama pull nomic-embed-text
ollama list   # confirme nomic-embed-text:latest presente
```

### 2. Subir a infraestrutura (PostgreSQL+pgvector, Redis, RabbitMQ, AI Services)

```powershell
cd sky-monolith
docker compose up -d postgres redis rabbitmq ai-services
docker compose ps          # todos healthy
```

Validar:
- PostgreSQL: `docker exec skymetron-postgres psql -U skymetron -d skymetron -c "SELECT extname FROM pg_extension WHERE extname='vector';"`
- AI Services: `curl http://localhost:8001/health`
- RabbitMQ UI: http://localhost:15672 (`skymetron` / `skymetron`)

### 3. Rodar o SkyMetron Core

**Opção A — Docker (tudo junto):**
```powershell
cd sky-monolith
docker compose up -d --build sky-core
curl http://localhost:8080/actuator/health
```

**Opção B — Local (dev):**
```powershell
cd sky-monolith
mvn clean spring-boot:run -pl sky-core
```

### 4. Health checks e métricas

| Endpoint | URL |
|----------|-----|
| Health | `GET http://localhost:8080/actuator/health` |
| Métricas | `GET http://localhost:8080/actuator/metrics` |
| Prometeus | `GET http://localhost:8080/actuator/prometheus` |
| Info | `GET http://localhost:8080/actuator/info` |

### 5. Testar embeddings end-to-end

```powershell
curl -X POST http://localhost:8001/api/embeddings `
  -H "Content-Type: application/json" `
  -d '{\"text\":\"SkyMetron eh um sistema operacional de IA\"}'
```

Resposta esperada: `{"embedding":[...768 floats...],"model":"nomic-embed-text","dimensions":768}`

---

## Build e Testes

```powershell
# Compilar todo o monolito
cd sky-monolith
mvn clean install -DskipTests

# Testes unitários (rápidos, sem serviços externos) — roda no CI
mvn test

# Build completo (unit + pacote) — verde por padrão
mvn verify

# Testes de integração (requer Docker/Testcontainers) — opt-in
mvn verify -Pintegration -pl sky-core
```

> **Nota (Testcontainers no Windows):** Se o Testcontainers não localizar o Docker,
> configure `~/.testcontainers.properties` com `docker.host=npipe:////./pipe/dockerDesktopLinuxEngine`
> (ou o pipe do contexto ativo: `docker context ls`).

---

## Configuração

Toda configuração sensível vem de variáveis de ambiente (ver `sky-monolith/sky-core/src/main/resources/application.yml`).

| Variável | Default | Descrição |
|----------|---------|-----------|
| `SKY_DB_HOST` | `localhost` | Host PostgreSQL |
| `SKY_DB_PORT` | `5432` | Porta PostgreSQL |
| `SKY_DB_NAME` | `skymetron` | Database |
| `SKY_DB_USER` / `SKY_DB_PASSWORD` | `skymetron` | Credenciais |
| `SKY_REDIS_HOST` / `SKY_REDIS_PORT` | `localhost:6379` | Redis |
| `SKY_RABBIT_HOST` / `SKY_RABBIT_PORT` | `localhost:5672` | RabbitMQ |
| `SKY_AI_SERVICES_URL` | `http://localhost:8001` | Python AI service |
| `SKY_OLLAMA_URL` | `http://localhost:11434` | Ollama local |
| `SKY_EMBEDDING_MODEL` | `nomic-embed-text` | Modelo de embeddings |

---

## Estrutura de Pacotes (Bounded Contexts)

```
dev.skymetron
├── domain/          # BC 1-6: identity, memory, execution, knowledge, observation, tool
├── application/     # usecase, port (in/out), agent, provider, scheduler
└── infrastructure/  # persistence (jpa, pgvector), messaging (rabbitmq), ai (ollama), tool, web
```

## Decisões de Arquitetura (ADRs)

- [ADR-001](sky-docs/adr/ADR-001-primary-database.md) — PostgreSQL 16 + pgvector
- [ADR-002](sky-docs/adr/ADR-002-event-bus.md) — RabbitMQ (Fase 0-8)
- [ADR-003](sky-docs/adr/ADR-003-llm-providers.md) — Multi-provider 100% free
- [ADR-004](sky-docs/adr/ADR-004-embedding-model.md) — nomic-embed-text via Ollama

---

## Roadmap

Ver `SKYMETRON.MD` (Protocolo 0 — Plano Definitivo v4). Sprint atual: **0 — Fundação**.

**In Veritas, Fundamentum.**
