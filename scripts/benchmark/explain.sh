#!/usr/bin/env bash
# Usage: scripts/benchmark/explain.sh "<search terms>"
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; source .env; set +a

docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v terms="$1" -f - \
  < scripts/benchmark/explain-search.sql