# Scaling Chronos horizontally

## How each tier scales

| Tier | How to scale | Why it is safe |
|---|---|---|
| **api** | N stateless replicas behind a load balancer | JWT auth, no server session; all state in PostgreSQL |
| **scheduler** | N replicas (`--scale scheduler=N`) | `SELECT ... FOR UPDATE SKIP LOCKED` hands each due job / pending instance to exactly one replica; no leader election, no distributed lock |
| **worker** | N replicas, plus `WORKER_CONCURRENCY` threads each | RabbitMQ competing consumers; the idempotent `PENDING/QUEUED -> RUNNING` conditional UPDATE means a duplicate delivery is a no-op |
| **RabbitMQ** | Cluster / quorum queues | Messages are only a *hint*; the DB row is the source of truth, so a lost message is re-published after `requeueStaleQueued` |
| **PostgreSQL** | The real ceiling (see below) | Single writer; partial indexes keep hot queries index-only on small sets |

Design concepts used: transactional outbox, competing consumers, idempotent consumer, `SKIP LOCKED` work-queue,
at-least-once delivery + idempotency = effectively-once execution, durable retries (retry = a future-dated row),
reaper for stuck work, circuit breaker per target host, SSRF guard, batched retention deletes, atomic SQL counters
(no read-modify-write), optimistic locking on edits.

## Known limits (be honest about these in interviews)

1. **PostgreSQL is the bottleneck**, not the services: every run is ~4-6 row writes (instance, claim, complete, job counters, log).
   Expect the DB to saturate before 4+ workers do.
2. `job_logs` and `job_instances` grow without bound until retention runs; at high volume partition them by month.
3. A hot job (many runs per second) serialises on its own `jobs` row because of the counter UPDATE. Fix: move counters to a separate
   `job_stats` table or derive them from instances asynchronously.
4. At-least-once: a worker that dies *after* the HTTP call but *before* recording the result will be re-run by the reaper.
   Targets should be idempotent (send `X-Chronos-Instance-Id` if you need dedupe on their side).

## Next steps if this had to grow 10-100x

- Put **PgBouncer** in front of Postgres; add a **read replica** for the API's list/statistics queries.
- **Partition** `job_instances`/`job_logs` by time (drop old partitions instead of `DELETE`).
- **Shard** the scheduler by `job_id % K` (each replica owns shards) to cut lock contention.
- **Per-tenant rate limits / quotas** and priority queues in RabbitMQ.
- Move counters and logs to an append-only/async path (batch inserts or Kafka).

## Measuring it (do this, then fill the table with YOUR numbers)

```bash
cp .env.example .env            # set JWT_SECRET etc.
./bench/run.sh 50000 1 1        # N instances, schedulers, workers   (Windows: .\bench\run.ps1 -N 50000 -Schedulers 1 -Workers 1)
./bench/run.sh 50000 2 4
./bench/run.sh 50000 2 8
```
`report.sql` prints throughput, queue-wait p50/p95/p99, execution p50/p95, and three **correctness checks**
(`duplicate_runs`, `stuck_not_terminal`, `job_counter_mismatch` must all be 0).
API load: `docker run --rm -i --network host grafana/k6 run - < bench/k6/api-load.js`.
Prometheus is on http://localhost:9090 while the bench overlay is up.

Record results here (machine: CPU/RAM/Docker limits, date, commit):

| Schedulers | Workers | Concurrency | Instances | Wall s | Throughput/s | Queue wait p95 ms | Dup/lost |
|---|---|---|---|---|---|---|---|
| 1 | 1 | 8 | 50 000 | | | | |
| 2 | 4 | 8 | 50 000 | | | | |
| 2 | 8 | 8 | 50 000 | | | | |

Only quote numbers you measured, with the hardware and setup next to them. A laptop result with an
instant nginx target says "the framework overhead is X", not "it handles X jobs/s in production".

## Watching a run in Grafana

With the bench overlay up, open http://localhost:3000 (no login). Two data sources are pre-provisioned:
**Prometheus** (JVM/HTTP/Hikari/Rabbit metrics) and **Chronos-DB** (PostgreSQL, for the job metrics).
Create a dashboard (New -> Dashboard -> Add visualization) with these panels:

| Panel | Data source | Query |
|---|---|---|
| Throughput (runs/s) | Chronos-DB (time series) | `SELECT date_trunc('second', completed_at) AS time, count(*) AS runs FROM job_instances WHERE completed_at > now() - interval '15 minutes' GROUP BY 1 ORDER BY 1` |
| Backlog by status | Chronos-DB (time series, refresh 5s) | `SELECT now() AS time, status AS metric, count(*) AS value FROM job_instances WHERE status IN ('PENDING','QUEUED','RUNNING') GROUP BY status` |
| Queue wait p95 (ms) | Chronos-DB | `SELECT date_trunc('second', started_at) AS time, percentile_cont(0.95) WITHIN GROUP (ORDER BY extract(epoch FROM started_at - scheduled_time)*1000) AS p95 FROM job_instances WHERE started_at > now() - interval '15 minutes' GROUP BY 1 ORDER BY 1` |
| API request rate | Prometheus | `sum(rate(http_server_requests_seconds_count[30s]))` |
| API p95 latency | Prometheus | `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[1m])))` |
| DB pool pressure | Prometheus | `hikaricp_connections_active` and `hikaricp_connections_pending` by `job` |
| CPU per service | Prometheus | `process_cpu_usage` by `job` |

Take a screenshot of the throughput + backlog panels during a run: that is a good README/portfolio image.
