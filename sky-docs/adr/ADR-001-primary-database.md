# ADR-001: Primary Database

- **Status:** Accepted
- **Date:** 2026-06
- **Decision:** PostgreSQL 16 com pgvector

## Context

SkyMetron precisa de um banco principal que armazene:
- Memória persistente (fatos, contexto de sessão, conhecimento de projetos)
- Embeddings vetoriais para busca semântica
- Metadados dinâmicos (ECS — Entity-Component-System) sem schema rígido
- Eventos de trace/observabilidade

## Decision

**PostgreSQL 16** com a extensão **pgvector**.

## Rationale

- **JSONB nativo com índices** — ideal para componentes ECS de schema dinâmico (GIN).
- **Array types** — tags, skills, capabilities nativas.
- **Full text search** — `to_tsvector` / `tsquery` sem Elasticsearch adicional.
- **pg_trgm** — busca fuzzy para deduplicação.
- **Listen/Notify** — event bus simples sem RabbitMQ para casos locais.
- **pgvector** — elimina Qdrant na fase inicial. HNSW index: 2-5ms para até 100k vetores com 768 dimensões. Armazenamento: 50k vetores ≈ 150MB (trivial).

## Alternatives Considered

| Alternativa | Motivo da rejeição |
|-------------|--------------------|
| MySQL 8 | JSON menos maduro, sem tipo vetorial nativo, sem pg_trgm equivalente |
| Qdrant separado | Complexidade adicional desnecessária para <100K vetores |

## Review Trigger

Reavaliar pgvector vs. Qdrant separado apenas se:
- Vetores > 500.000
- Latência pgvector > 100ms p95
