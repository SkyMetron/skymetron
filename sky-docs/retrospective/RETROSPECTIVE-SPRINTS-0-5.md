# Retrospectiva Sprints 0-5

**Data:** Sprint 6 (após Spike de retrospectiva obrigatório a cada 3 sprints)
**Período:** Sprints 0-5

---

## Resumo

6 sprints completas (0-5). Sistema passou de infraestrutura vazia para um AI OS funcional com 5 agentes, 7 LLM providers, memória persistente com busca semântica, pesquisa web, deduplicação automática e loops autônomos.

## Dévida Técnica Documentada

### 1. JpaEntityStore JSONB Serialization (Sprint 2)
- **Problema:** `getComponents()` endpoint falha ao desserializar `CapabilityComponent` com `Set<String>` — Jackson default typing adiciona metadados de array que conflitam.
- **Impacto:** Baixo — o core ECS funciona (create, attach, query, list, delete). Apenas o endpoint de visualização de componentes falha.
- **Plano:** Sprint 7+ — usar `@JsonTypeInfo` customizado ou remover default typing e usar type hints manuais.

### 2. CEO Intent Classification por Keywords (Sprint 3-4)
- **Problema:** `classifyIntent()` usa keywords simples ("lembre", "pesquise", "codigo"). Funciona mas é frágil — mensagens ambíguas são mal classificadas.
- **Impacto:** Médio — pode rotear para agente errado.
- **Plano:** Sprint 8+ — usar LLM para classificação de intenção (prompt curto, FAST_RESPONSE).

### 3. Chunking de Documentos Longos (Sprint 1)
- **Problema:** Migração ENIAC Vault falhou para 233 documentos `.opencode/skills/SKILL.md` que excedem o contexto do nomic-embed-text.
- **Impacto:** Médio — conhecimento de skills não migrado.
- **Plano:** Sprint 8+ — implementar chunking de texto antes de embedding (sliding window de 512 tokens).

### 4. Testcontainers no Windows (Sprint 0)
- **Problema:** docker-java client não conecta ao Docker Desktop no Windows (pipe mismatch). Testes `*IT` não rodam localmente.
- **Impacto:** Baixo — testes rodam no CI (Linux). Testes unitários cobrem 100% da lógica.
- **Plano:** Documentar workaround no README. Considerar Testcontainers Desktop integration.

### 5. WebSearchTool Parsing Frágil (Sprint 5)
- **Problema:** `WebSearchTool` usa regex sobre HTML do DuckDuckGo. Se o HTML mudar, o parsing quebra.
- **Impacto:** Médio — pesquisa web pode parar sem aviso.
- **Plano:** Sprint 10+ — usar SearXNG local ou API gratuita alternativa. Considerar scraping com JSoup.

### 6. EventPublisher JSON String (Sprint 6)
- **Problema:** Eventos são publicados como JSON strings (não objetos serializados) porque Jackson não serializa records corretamente com default typing.
- **Impacto:** Baixo — funciona, mas perde type safety no consumer.
- **Plano:** Sprint 7+ — configurar ObjectMapper do RabbitMQ sem default typing, usar type headers.

### 7. Provider Status Initial State (Sprint 3)
- **Problema:** `ProviderStatus` começa com `available=false` até a primeira chamada. Providers cloud que estão configurados mas nunca foram chamados aparecem como indisponíveis.
- **Impacto:** Baixo — o `isAvailable()` do provider retorna true, mas o status no registry mostra false.
- **Plano:** Sprint 7+ — inicializar status chamando `isAvailable()` no construtor do ProviderRegistry (já feito parcialmente).

## O Que Funcionou Bem

- **Multi-provider com fallback:** 7 providers, fallback automático, zero custos.
- **ECS + DDD híbrido:** Entidades e componentes para agentes dinâmicos, DDD aggregates para Tasks/Memory.
- **pgvector:** Busca semântica em <100ms para 437 memórias. HNSW index excelente.
- **LoopScheduler:** Circuit breaker + pause/resume funcionando. Virtual threads para loops.
- **Migração ENIAC Vault:** 433 documentos migrados em 51.7s. Deduplicação semântica funcionou.

## O Que Melhorar

- **Observabilidade:** Brain Trace (Sprint 6) resolve a maior parte. Falta Prometheus metrics customizadas (Sprint 7).
- **Tratamento de erro em loops:** ConsolidationAgent pode falhar silenciosamente se LLM retornar texto inválido.
- **Rate limiting real:** SessionBudget conta tokens mas não respeita rate limits dos free tiers (RPM).
- **Streaming LLM:** Nenhum provider usa streaming ainda. Para respostas longas, a latência aparente é alta.

## Métricas

| Métrica | Valor |
|---------|-------|
| Sprints completas | 6 |
| Testes unitários | 118 |
| Agents funcionais | 5 (CEO, Memory, Tool, Research, Consolidation) |
| LLM Providers | 7 (Mistral, NVIDIA, Google, Groq, Cerebras, OpenRouter, Ollama) |
| Memórias no Vault | 437 |
| Eventos traceados | 17+ (Sprint 6) |
| Loops autônomos | 2 (health-check, memory-consolidation) |
| Custo mensal LLM | R$ 0,00 |
