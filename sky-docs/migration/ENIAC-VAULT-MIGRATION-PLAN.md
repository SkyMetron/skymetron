# ENIAC Vault — Plano de Migração para SkyMetron

- **Status:** Planejado (Sprint 0) · Execução no Sprint 1
- **Origem:** `ENIAC_METRON/` (arquivado, read-only)
- **Destino:** `sky-monolith/sky-vault` → PostgreSQL 16 + pgvector (`memory_entries`)

## 1. Inventário da Origem

| Localização | Conteúdo | Volume | Tipo |
|-------------|----------|--------|------|
| `ENIAC_METRON/vault/ARCHITECTURE/` | Planos, schema de intenção, ADRs legados | ~6 arquivos | `project_knowledge` |
| `ENIAC_METRON/vault/99-ARCHIVE/` | Inventários de agentes, benchmarks, relatórios de deps | ~5 arquivos | `project_knowledge` (histórico) |
| `ENIAC_METRON/vault/ENIAC-Knowledge/` | Regras, sessões/resumos de chats | ~3 arquivos | `session_context`, `user_facts` |
| `ENIAC_METRON/knowledge/` | Base Obsidian (ENIAC-Knowledge, 12-Regras) | **~558 arquivos .md** | `project_knowledge`, `skill_embeddings` |
| `ENIAC_METRON/.eniac/vault/` | README do vault | 1 arquivo | metadado |

**Total estimado:** ~570 documentos markdown + JSONs de configuração.

## 2. Fases da Migração

### Fase 1 — Inventário (Sprint 1, início)

- [ ] Listar todos os documentos das 3 origens (`vault/`, `knowledge/`, `.eniac/vault/`).
- [ ] Classificar cada documento por tipo: `user_facts`, `project_knowledge`, `session_context`, `skill_embeddings`.
- [ ] Extrair metadados (título, data, tags Obsidian, links internos `[[...]]`).
- [ ] Identificar duplicatas por hash de conteúdo (SHA-256 do texto normalizado).
- [ ] Gerar relatório `migration_inventory.json` (caminho, tipo, hash, tamanho).

### Fase 2 — Validação

- [ ] Atribuir `confidence` a cada fato (1.0 = explícito e recente; 0.5 = inferido/histórico).
- [ ] Marcar fatos desatualizados (referências a METRON legacy, stack antigo) para revisão.
- [ ] Identificar conteúdo a descartar (logs, caches, backups temporários).
- [ ] Resolver links internos `[[wiki-links]]` Obsidian → referências por ID/título.

### Fase 3 — Transformação

- [ ] Mapear para o schema `memory_entries`:
  - `content` ← texto markdown limpo (frontmatter YAML → `metadata`).
  - `metadata` ← JSONB `{type, tags, source_path, original_title, obsidian_links[]}`.
  - `source` ← `"eniac-vault-migration"`.
  - `confidence` ← valor da Fase 2.
  - `embedding` ← gerado por `nomic-embed-text` (via `sky-ai-services`).
- [ ] Reindexar **todos** os embeddings com `nomic-embed-text` (768 dims) — descartar embeddings antigos.

### Fase 4 — Importação

- [ ] Importar em lotes de 100 documentos (respeitando rate limit do Ollama local).
- [ ] Validar integridade: cada entrada tem `embedding` não-nulo de 768 dims.
- [ ] Executar deduplicação inicial (similaridade > 0.95 → `merged_into`).
- [ ] Gerar relatório final `migration_report.json`:
  - total importado / descartado / mergeado por tipo
  - entradas com baixa confiança marcadas para revisão
- [ ] Log de deduplicação no Brain Trace.

## 3. Critérios de Descarte

Serão **descartados** (não migrados):
- Arquivos em `logs/`, `tmp/`, `cache/` (dados de runtime, não conhecimento).
- Relatórios gerados automaticamente sem valor semântico (e.g., `agent_coverage_report.json` cru).
- Backups `.zip`, `.bak`, `.dump`.

Serão **preservados como `project_knowledge` (histórico)**:
- ADRs legados (com `confidence=0.7`, marcados como superseded).
- Benchmarks e relatórios de decisão arquitetural.

## 4. Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| Links Obsidian `[[...]]` quebrados | Resolver na Fase 3; manter `original_title` em `metadata` |
| Embeddings de texto em PT com qualidade variável | `nomic-embed-text` é multilíngue; aceitável per ADR-004 |
| Volume de 558 docs sobrecarregar Ollama | Importar em lotes de 100; Ollama local sem rate limit |
| Duplicatas entre `vault/` e `knowledge/` | Deduplicação semântica (>0.95) na Fase 4 |

## 5. Executável

Implementação Java: `sky-monolith/sky-vault/migration/EniacVaultMigration.java` (Sprint 1).
