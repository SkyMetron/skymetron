# SkyMetron

> AI Operating System — permanent memory, specialized agents, controlled autonomy,
> multi-provider LLM **100% free** (R$ 0,00/mês).

**Status:** v0.1.0-beta — [Release](https://github.com/SkyMetron/skymetron/releases/tag/v0.1.0-beta)

---

## Ecossistema de Repositórios

| Repositório | Visibilidade | Descrição |
|-------------|-------------|-----------|
| **[skymetron](https://github.com/SkyMetron/skymetron)** | Público | Core Java + Desktop + API + CI/CD |
| **[skymetron-ai-services](https://github.com/SkyMetron/skymetron-ai-services)** | Privado | Serviços auxiliares Python/FastAPI, Ollama Bridge, Embeddings |
| **[skymetron-docs](https://github.com/SkyMetron/skymetron-docs)** | Privado | ADRs, documentação, arquitetura, roadmap |
| **[skymetron-deployment](https://github.com/SkyMetron/skymetron-deployment)** | Privado | Scripts, Docker Compose produção, infraestrutura |
| **[skymetron-monitoring](https://github.com/SkyMetron/skymetron-monitoring)** | Privado | Grafana, Prometheus, Loki — observabilidade |
| **[sky-vault](https://github.com/SkyMetron/sky-vault)** | Privado | Knowledge base — vault Obsidian compartilhado (575+ markdowns, arquitetura, skills, sessões) |
| **[skymetron-vault](https://github.com/SkyMetron/skymetron-vault)** | Privado | Configuração do vault vetorial, embeddings, dados persistidos |
| **[skymetron-secrets](https://github.com/SkyMetron/skymetron-secrets)** | Privado | Templates de gerenciamento de secrets (nunca contém secrets reais) |

---

## Estrutura deste Repositório

```
skymetron/
├── sky-monolith/         # Java 21 + Spring Boot 3 + Maven (core, vault, ai-bridge)
├── sky-desktop/          # Electron + React + TypeScript + Vite
├── .github/workflows/    # CI + Release workflows
└── CHANGELOG.md          # Histórico completo de versões
```

---

## Pré-requisitos

| Ferramenta | Versão mínima | Verificar |
|-----------|---------------|-----------|
| Java (JDK) | 21 LTS | `java -version` |
| Maven | 3.9 | `mvn -version` |
| Docker + Compose | 24+ / v2 | `docker compose version` |
| Ollama | 0.1+ | `ollama --version` |

## Setup — passo a passo

### 1. Ollama + modelo de embeddings

```powershell
ollama pull nomic-embed-text
ollama list
```

### 2. Subir infraestrutura

```powershell
cd sky-monolith
docker compose up -d postgres redis rabbitmq
docker compose ps
```

### 3. Rodar AI Services

```powershell
# A partir do repositório skymetron-ai-services (ou via Docker)
git clone https://github.com/SkyMetron/skymetron-ai-services.git
cd skymetron-ai-services
pip install -r requirements.txt
python main.py
```

### 4. Rodar o SkyMetron Core

**Docker:**
```powershell
cd sky-monolith
docker compose up -d --build sky-core
```

**Local (dev):**
```powershell
cd sky-monolith
mvn clean spring-boot:run -pl sky-core
```

---

## Build e Testes

```powershell
cd sky-monolith
mvn clean install -DskipTests  # compilar
mvn test                       # testes unitários
mvn verify                     # build completo
```

---

## Configuração

Todas as variáveis de ambiente obrigatórias estão documentadas em [`.env.example`](.env.example). Copie e preencha:

```powershell
cp .env.example .env
# edite .env com suas credenciais e API keys
```

Variáveis principais (ver `sky-core/src/main/resources/application.yml`):

| Variável | Obrigatória | Descrição |
|----------|:-----------:|-----------|
| `POSTGRES_PASSWORD` | ✅ | Senha do PostgreSQL |
| `RABBITMQ_PASSWORD` | ✅ | Senha do RabbitMQ |
| `SKY_JWT_SECRET` | ✅ | Chave secreta JWT (32 bytes) |
| `SKY_ENCRYPTION_KEY` | ✅ | Chave mestra AES/GCM (base64, 32 bytes) |
| `SKY_DB_HOST` | — | Host PostgreSQL (default: embutido no Docker) |
| `SKY_DB_PORT` | — | Porta PostgreSQL (default: `5432`) |
| `SKY_DB_NAME` | — | Database (default: `skymetron`) |
| `SKY_DB_USER` / `SKY_DB_PASSWORD` | — | Credenciais DB (herdam de `POSTGRES_*` no Docker) |
| `SKY_REDIS_HOST` / `SKY_REDIS_PORT` | — | Redis |
| `SKY_RABBIT_HOST` / `SKY_RABBIT_PORT` | — | RabbitMQ |
| `SKY_AI_SERVICES_URL` | — | Python AI service |
| `SKY_OLLAMA_URL` | — | Ollama local |
| `SKY_EMBEDDING_MODEL` | — | Modelo de embeddings (default: `nomic-embed-text`) |
| `SKY_DEV_ADMIN_PASSWORD` | — | Senha do usuário dev admin (vazio = desabilitado) |
| `SKY_CORS_ORIGINS` | — | Origins CORS permitidas (comma-separated) |
| `VITE_API_BASE_URL` | — | URL base da API no Desktop (default: `http://localhost:8080`) |

## Documentação

- [Arquitetura e ADRs](https://github.com/SkyMetron/skymetron-docs)
- [Guia de Deploy](https://github.com/SkyMetron/skymetron-deployment)
- [Upgrade e Versionamento](https://github.com/SkyMetron/skymetron-deployment)
- [Observabilidade](https://github.com/SkyMetron/skymetron-monitoring)
- [CHANGELOG](./CHANGELOG.md)

---

**In Veritas, Fundamentum.**
