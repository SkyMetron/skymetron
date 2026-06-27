# ADR-004: Embedding Model

- **Status:** Accepted
- **Date:** 2026-06
- **Decision:** `nomic-embed-text` via Ollama Local

## Context

SkyMetron precisa de embeddings para busca semântica no Vault (pgvector). O modelo deve ser gratuito, funcionar offline, ter boa qualidade (inclusive em português) e dimensões adequadas ao índice HNSW.

## Decision

**`nomic-embed-text`** servido localmente via **Ollama**.

| Atributo | Valor |
|----------|-------|
| Dimensões | 768 |
| Tamanho | ~274MB |
| Qualidade EN | Excelente (MTEB top 10) |
| Qualidade PT | Bom (multilíngue) |
| Custo | R$0,00 |
| Latência | ~5ms por embedding (CPU local) |
| Offline | Sim |

## Rationale

- **Tamanho** — ~274MB carrega em qualquer desktop.
- **Dimensão 768** — ponto ideal para pgvector HNSW (m=16, ef_construction=64).
- **Qualidade** — benchmark MTEB top 10 global; desempenho aceitável em português.
- **Integração** — disponível nativamente no Ollama.
- **Sem dependência externa** — funciona offline.

## Alternatives Considered

| Alternativa | Motivo da rejeição |
|-------------|--------------------|
| OpenAI text-embedding-3-small | Custo mensal |
| sentence-transformers all-MiniLM-L6-v2 | Ruim em português (384 dim) |
| bge-large-en-v1.5 | Pesado (~1.3GB), ruim em português |
| mxbai-embed-large | Maior (1024 dim, ~670MB), sem ganho proporcional |
