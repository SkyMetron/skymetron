# ADR-002: Event Bus

- **Status:** Accepted
- **Date:** 2026-06
- **Decision:** RabbitMQ (Fase 0-8)

## Context

SkyMetron precisa de um event bus assíncrono entre componentes: agentes, loops autônomos, trace, consolidação de memória. O barramento deve suportar dead-letter queues, retry e observabilidade.

## Decision

**RabbitMQ** para as Fases 0-8. Reavaliar Kafka apenas em gatilhos específicos.

## Rationale

- Simples, eficaz, suficiente para volume local.
- Dead-letter queues nativas.
- Management UI pronta (porta 15672).
- Spring AMQP maduro e integrado ao Spring Boot.
- Suporta pub/sub, routing keys, work queues.

## Alternatives Considered

| Alternativa | Motivo da rejeição (fase atual) |
|-------------|---------------------------------|
| Kafka | Overkill para volume local; justificável apenas com replay/multi-instância |
| PostgreSQL Listen/Notify | Suficiente para casos locais pontuais, mas não substitui RabbitMQ para DLQ/retry |

## Review Trigger

Reavaliar para Kafka apenas se:
- Replay de eventos necessário
- Volume > 10.000 mensagens/segundo
- Deployment multi-instância
- Auditoria completa (event sourcing) requerida
