#!/usr/bin/env bash
# Seeds the local database with synthetic INDEXADO documents for search benchmarks.
# Usage: scripts/seed/seed.sh [count]   (default: 50000)
set -euo pipefail
COUNT="${1:-50000}"
cd "$(dirname "$0")/../.."
set -a; source .env; set +a

echo "Seeding ${COUNT} documents..."
time docker compose exec -T postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -v count="$COUNT" -q -f - \
  < scripts/seed/seed-documents.sql