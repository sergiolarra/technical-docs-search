#!/usr/bin/env bash
# Removes the synthetic documents created by seed.sh. Real uploads are kept.
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; source .env; set +a

docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
  -c "DELETE FROM documents WHERE starts_with(storage_key, 'seed-')" \
  -c "VACUUM ANALYZE documents"