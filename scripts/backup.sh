#!/usr/bin/env bash
set -euo pipefail

# SkyMetron Backup Script
# Backs up PostgreSQL (with pgvector) and application data.
#
# Usage:
#   ./scripts/backup.sh                    # backup to ./backups/
#   ./scripts/backup.sh /path/to/dir       # backup to custom directory
#
# For Docker deployments:
#   docker exec skymetron-postgres pg_dump -U skymetron -d skymetron -Fc > backup.dump
# Or using this script with Docker compose:
#   docker compose -f docker-compose.prod.yml exec -T postgres pg_dump -U skymetron -d skymetron -Fc > backup.dump

BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DB_NAME="${SKY_DB_NAME:-skymetron}"
DB_USER="${SKY_DB_USER:-skymetron}"
DB_HOST="${SKY_DB_HOST:-localhost}"
DB_PORT="${SKY_DB_PORT:-5432}"
PGPASSWORD="${SKY_DB_PASSWORD:-skymetron}"
export PGPASSWORD

mkdir -p "$BACKUP_DIR"

echo "→ Starting backup to $BACKUP_DIR ..."

# Full database dump (custom format, compressed)
DUMP_FILE="$BACKUP_DIR/skymetron_db_$TIMESTAMP.dump"
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -Fc --no-owner --no-privileges \
  -f "$DUMP_FILE"
echo "  ✓ Database dump: $DUMP_FILE ($(du -h "$DUMP_FILE" | cut -f1))"

# Schema-only dump (for reference)
SCHEMA_FILE="$BACKUP_DIR/skymetron_schema_$TIMESTAMP.sql"
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  --schema-only --no-owner --no-privileges \
  -f "$SCHEMA_FILE"
gzip -f "$SCHEMA_FILE"
echo "  ✓ Schema dump: $SCHEMA_FILE.gz"

# Compress the custom dump
gzip -f "$DUMP_FILE"
echo "  ✓ Compressed: $DUMP_FILE.gz"

echo "→ Backup complete."
echo ""
echo "To restore, run:"
echo "  gunzip -c $DUMP_FILE.gz | pg_restore -U $DB_USER -d $DB_NAME --no-owner --no-privileges"
echo ""
echo "Or:"
echo "  ./scripts/restore.sh $DUMP_FILE.gz"
