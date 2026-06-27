#!/usr/bin/env bash
set -euo pipefail

# SkyMetron Restore Script
# Restores a PostgreSQL backup (pg_dump custom format).
#
# Usage:
#   ./scripts/restore.sh backups/skymetron_db_20240101_120000.dump.gz
#
# For Docker deployments:
#   gunzip -c backup.dump.gz | docker exec -i skymetron-postgres pg_restore -U skymetron -d skymetron --no-owner --no-privileges
#
# WARNING: This will overwrite the current database. Use with caution.

if [ $# -lt 1 ]; then
  echo "Usage: $0 <backup-file.dump.gz>"
  echo ""
  echo "Examples:"
  echo "  $0 backups/skymetron_db_20240101_120000.dump.gz"
  echo "  $0 latest  # restores the most recent backup from ./backups/"
  exit 1
fi

BACKUP_FILE="$1"
DB_NAME="${SKY_DB_NAME:-skymetron}"
DB_USER="${SKY_DB_USER:-skymetron}"
DB_HOST="${SKY_DB_HOST:-localhost}"
DB_PORT="${SKY_DB_PORT:-5432}"
PGPASSWORD="${SKY_DB_PASSWORD:-skymetron}"
export PGPASSWORD

if [ "$BACKUP_FILE" = "latest" ]; then
  BACKUP_FILE=$(ls -t ./backups/skymetron_db_*.dump.gz 2>/dev/null | head -1)
  if [ -z "$BACKUP_FILE" ]; then
    echo "! No backups found in ./backups/"
    exit 1
  fi
  echo "→ Using latest backup: $BACKUP_FILE"
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "! Backup file not found: $BACKUP_FILE"
  exit 1
fi

echo "╔══════════════════════════════════════════════════╗"
echo "║   WARNING: This will OVERWRITE the database!    ║"
echo "║   Database: $DB_NAME@$DB_HOST:$DB_PORT          ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""
read -rp "Type 'yes' to continue: " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Aborted."
  exit 0
fi

echo "→ Starting restore from $BACKUP_FILE ..."

# Terminate existing connections
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT pg_terminate_backend(pg_stat_activity.pid)
       FROM pg_stat_activity
       WHERE pg_stat_activity.datname = '$DB_NAME'
         AND pid <> pg_backend_pid();" 2>/dev/null || true

# Restore the dump
gunzip -c "$BACKUP_FILE" | pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  --no-owner --no-privileges --clean --if-exists \
  --exit-on-error

echo ""
echo "✓ Restore complete!"
echo ""
echo "Post-restore steps:"
echo "  1. Restart sky-core: docker restart skymetron-core (if using Docker)"
echo "  2. Verify: curl http://localhost:8080/actuator/health"
echo "  3. Check memory count: curl http://localhost:8080/api/memory/count"
