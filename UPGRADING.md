# Upgrading SkyMetron

## Version Scheme

SkyMetron uses [Semantic Versioning](https://semver.org/):

- **MAJOR** (v1.x.x): Breaking changes — database migrations, config format changes
- **MINOR** (v0.1.x): New features — incompatible API changes possible but documented
- **PATCH** (v0.x.1): Bug fixes and minor improvements — backwards compatible

## Upgrade Process

### 1. Desktop App (Auto-Update)

The desktop app checks for updates on startup via GitHub Releases:

```bash
# Manual check — click "Check for Updates" in Settings
# Or run:
cd sky-desktop
npm run publish
```

Update status is displayed in the Settings page.

### 2. Docker Stack (Manual)

```bash
# Pull latest images
docker compose -f docker-compose.prod.yml pull

# Recreate containers
docker compose -f docker-compose.prod.yml up -d --remove-orphans

# Check health
curl http://localhost:8080/actuator/health
```

### 3. Backend JAR (Manual)

```bash
# Download the latest JAR from GitHub Releases
curl -L "https://github.com/anomalyco/SkyMetron/releases/latest/download/sky-core.jar" \
  -o sky-core.jar

# Stop existing service
systemctl stop skymetron

# Replace JAR and restart
cp sky-core.jar /opt/skymetron/
systemctl start skymetron
```

## Database Migrations

All database changes use Flyway (`classpath:db/migration/`).

- Migrations run automatically on startup
- They are versioned and irreversible
- Rollback requires restoring a database backup

## Rollback Strategy

### Desktop
1. Open SysTray → Settings → "Check for Updates"
2. If issue persists: download previous version from GitHub Releases
3. Reinstall the older version (data is preserved)

### Docker
```bash
# Revert to specific version
docker compose -f docker-compose.prod.yml down
export SKYMETRON_VERSION=v0.0.9

# Edit compose file to use specific tag, then:
docker compose -f docker-compose.prod.yml up -d
```

### Database
```bash
# Full restore from backup
./scripts/restore.sh backups/skymetron_db_20240101_120000.dump.gz
```

## Pre-Upgrade Checklist

- [ ] Review [CHANGELOG.md](./CHANGELOG.md) for breaking changes
- [ ] Take a database backup: `./scripts/backup.sh`
- [ ] Check disk space: `df -h`
- [ ] Verify current health: `curl http://localhost:8080/actuator/health`
- [ ] Notify users of expected downtime

## Post-Upgrade Checklist

- [ ] Verify health: `curl http://localhost:8080/actuator/health`
- [ ] Check Flyway migrations: `curl http://localhost:8080/actuator/flyway`
- [ ] Run system diagnostics: `curl http://localhost:8080/api/admin/benchmark`
- [ ] Check memory profile: `curl http://localhost:8080/api/admin/memory`
- [ ] Verify desktop connects successfully
- [ ] Test a memory store and search query
