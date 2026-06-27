# SkyMetron Deployment Guide

## Architecture

```
┌─────────────────┐     ┌──────────────────────────────────────────┐
│  Desktop App    │     │  Docker Stack (Production)                │
│  (Electron +    │────▶│  ┌────────┐ ┌──────┐ ┌──────────┐       │
│   React + Vite) │     │  │  Core  │ │ AI   │ │ RabbitMQ │       │
│                  │     │  │ (Java) │ │(Python)│ │ (Events) │       │
│  Auto-update via │     │  └───┬────┘ └──────┘ └──────────┘       │
│  GitHub Releases │     │      │                                    │
└─────────────────┘     │  ┌────▼────┐ ┌──────┐                    │
                        │  │Postgres │ │Redis │                    │
                        │  │(pgvector)│ │(Cache│                    │
                        │  └─────────┘ └──────┘                    │
                        └──────────────────────────────────────────┘

Ollama runs on the host machine (not in Docker).
Observability stack (Grafana/Prometheus) is optional.
```

## Prerequisites

- Docker & Docker Compose v2
- Java 21 (for local JAR deployment) or Docker
- Node.js 20 (for desktop build)
- PostgreSQL 16 with pgvector extension (when not using Docker)
- Ollama (running locally, not in Docker)
  - Required models: `nomic-embed-text`, `llama3.1:8b`

## Quick Start (Docker)

### 1. Clone & Configure

```bash
git clone https://github.com/anomalyco/SkyMetron.git
cd SkyMetron

# Create .env file
cat > .env << 'EOF'
SKY_DB_PASSWORD=your-strong-password-here
SKY_JWT_SECRET=base64-32+byte-secret-here
SKY_ENCRYPTION_KEY=base64-32-byte-key-here
SKY_RABBIT_PASSWORD=rabbitmq-password
GRAFANA_PASSWORD=admin-password
EOF
```

### 2. Start Stack

```bash
docker compose -f docker-compose.prod.yml up -d

# Check health
curl http://localhost:8080/actuator/health
```

### 3. Authenticate

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# Returns: {"token":"eyJ...", "username":"admin", "roles":["ADMIN","USER"]}
```

### 4. Monitor

```bash
# Core health
curl http://localhost:8080/actuator/health

# Threads & memory
curl http://localhost:8080/api/admin/threads
curl http://localhost:8080/api/admin/memory

# Agent latencies
curl http://localhost:8080/api/admin/benchmark

# Logs
docker compose -f docker-compose.prod.yml logs -f sky-core
```

## Desktop App

### Build for distribution

```bash
cd sky-desktop

# Linux AppImage
npm run electron:build:linux

# macOS DMG
npm run electron:build:mac

# Windows NSIS installer
npm run electron:build:win

# Output: sky-desktop/release/
```

### Publish new version

```bash
# Build + publish to GitHub Releases
npm run publish
```

## Manual (JAR) Deployment

```bash
# Build
cd sky-monolith
mvn clean package -DskipTests

# Run
java -jar sky-core/target/sky-core-0.1.0-SNAPSHOT.jar
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SKY_DB_HOST` | localhost | PostgreSQL host |
| `SKY_DB_PORT` | 5432 | PostgreSQL port |
| `SKY_DB_NAME` | skymetron | Database name |
| `SKY_DB_USER` | skymetron | Database user |
| `SKY_DB_PASSWORD` | skymetron | Database password |
| `SKY_REDIS_HOST` | localhost | Redis host |
| `SKY_REDIS_PORT` | 6379 | Redis port |
| `SKY_RABBIT_HOST` | localhost | RabbitMQ host |
| `SKY_RABBIT_PORT` | 5672 | RabbitMQ port |
| `SKY_RABBIT_USER` | skymetron | RabbitMQ user |
| `SKY_RABBIT_PASSWORD` | skymetron | RabbitMQ password |
| `SKY_AI_SERVICES_URL` | http://localhost:8001 | AI services endpoint |
| `SKY_OLLAMA_URL` | http://localhost:11434 | Ollama endpoint |
| `SKY_EMBEDDING_MODEL` | nomic-embed-text | Embedding model |
| `SKY_JWT_SECRET` | — | JWT signing secret (32+ bytes, base64) |
| `SKY_ENCRYPTION_KEY` | — | Master encryption key (32 bytes, base64) |
| `SKY_CORE_PORT` | 8080 | HTTP server port |

## Healthcheck Endpoints

| Endpoint | Auth | Description |
|----------|------|-------------|
| `GET /actuator/health` | None | Liveness/readiness probes |
| `GET /actuator/info` | None | Application info |
| `GET /actuator/metrics` | ADMIN | Micrometer metrics |
| `GET /actuator/prometheus` | ADMIN | Prometheus scrape endpoint |
| `GET /actuator/flyway` | ADMIN | Flyway migrations status |
| `GET /api/admin/memory` | ADMIN | JVM memory usage |
| `GET /api/admin/threads` | ADMIN | Thread information |
| `GET /api/admin/benchmark` | ADMIN | Agent latency benchmarks |

## Security Checklist

- [ ] `SKY_JWT_SECRET` set to a cryptographically random 32+ byte value
- [ ] `SKY_ENCRYPTION_KEY` set to a 32-byte cryptographically random value (AES-256)
- [ ] Default passwords changed (`admin123`, `user123`, `readonly123`)
- [ ] Ports `5432`, `6379`, `5672` bound to `127.0.0.1` only (prod compose does this)
- [ ] HTTPS configured behind reverse proxy (nginx/caddy)
- [ ] Regular backups configured (`./scripts/backup.sh` in cron)
- [ ] Docker containers run as non-root users

## Troubleshooting

### "Connection refused" to PostgreSQL
```bash
docker compose -f docker-compose.prod.yml logs postgres
# Check if port is already in use
netstat -tlnp | grep 5432
```

### Flyway migration fails
```bash
# Check migration status
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/actuator/flyway

# Manual repair if needed
docker exec skymetron-postgres psql -U skymetron -d skymetron \
  -c "DELETE FROM flyway_schema_history WHERE success=false;"
```

### Desktop can't connect to Core
```bash
# Verify core is running
curl http://localhost:8080/actuator/health

# Check CORS config (desktop expects localhost:5173 or file://)
# Edit sky-monolith application.yml if needed
```
