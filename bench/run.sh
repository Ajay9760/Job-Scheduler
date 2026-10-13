#!/usr/bin/env bash
# ./bench/run.sh [N=50000] [SCHEDULERS=2] [WORKERS=4]   (needs a .env, see .env.example)
set -euo pipefail
N=${1:-50000}; S=${2:-2}; W=${3:-4}
DC="docker compose -f docker-compose.yml -f docker-compose.bench.yml"
PSQL="$DC exec -T postgres psql -U chronos -d chronos -v ON_ERROR_STOP=1"
$DC up -d --build --scale scheduler="$S" --scale worker="$W"
until $PSQL -c "select 1 from jobs limit 1" >/dev/null 2>&1; do sleep 2; done   # Flyway done
$PSQL -c "DELETE FROM jobs WHERE created_by='bench'" >/dev/null
$PSQL -v n="$N" < bench/seed.sql
echo "seeded $N due instances; waiting for drain (schedulers=$S workers=$W)..."
while :; do
  left=$($PSQL -At -c "select count(*) from job_instances i join jobs j on j.id=i.job_id where j.created_by='bench' and i.status in ('PENDING','QUEUED','RUNNING')")
  echo "remaining: $left"; [ "$left" -eq 0 ] && break; sleep 3
done
$PSQL -x < bench/report.sql
