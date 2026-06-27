# ADR-003: LLM Provider Strategy

- **Status:** Accepted
- **Date:** 2026-06
- **Decision:** Multi-provider 100% gratuito com roteamento pelo CeoAgent

## Context

Restrição orçamentária: **R$ 0,00/mês**. Nenhuma API key paga. O sistema deve funcionar inteiramente com free tiers e modelos locais, sem ponto único de falha.

## Decision

Arquitetura **ProviderRegistry** + **CeoAgent** com roteamento por tipo de tarefa e cadeia de fallback global.

### Provedores suportados

| Provedor | Modelo principal | Contexto | Custo |
|----------|-----------------|----------|-------|
| NVIDIA (free) | llama-3.1-nemotron-70b-instruct | 128K | R$0 |
| Mistral (free) | mistral-large-latest | 128K | R$0 |
| Google Gemini (free) | gemini-2.0-flash | 1M | R$0 |
| Groq (free) | llama-3.3-70b-versatile | 128K | R$0 |
| OpenRouter (free) | meta-llama/llama-3.1-8b-instruct:free | 128K | R$0 |
| Ollama Local | llama3.1 | 128K | R$0 |
| OpenCode Go (pago) | default | — | Último recurso |

### Cadeia de fallback

```
Mistral → NVIDIA → Gemini → Groq → OpenRouter → Ollama Local → ERRO claro
```

## Rationale

- **Zero custo** — todos os free tiers.
- **Resiliência** — 6 provedores antes do erro.
- **Offline** — Ollama Local funciona sem internet.
- **Roteamento inteligente** — CeoAgent escolhe o melhor provider por tipo de tarefa (código, análise longa, resposta rápida, etc.).

## Rule

Loops autônomos usam no máximo **20%** do rate limit. Os outros **80%** ficam reservados para interação do usuário.
