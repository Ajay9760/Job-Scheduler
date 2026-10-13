# Chronos: Distributed Job Scheduler

A learning project: schedule HTTP calls (cron, interval or one-off) and have them executed reliably by a pool of workers.
Java 21, Spring Boot 3.3, PostgreSQL, RabbitMQ, React (Vite).

> **Status:** well-structured and designed for correctness, but not battle-tested. It has unit tests and a CI pipeline;
> it has not been load-tested or run in production. See "Known limitations".

## Architecture

```
React UI --> api-service --------> PostgreSQL <-------- scheduler-service --> RabbitMQ --> worker-service
             (REST, JWT)           (source of truth)    (trigger, dispatch,                 (HTTP calls)
                                                         reaper, retention)
```

* `chronos-common`: entities, **the only Flyway schema**, lifecycle rules (`JobLifecycleService`), schedule maths, SSRF guard, queue topology.
* `api-service`: auth and owner-scoped REST API.
* `scheduler-service`: turns due jobs into `PENDING` instances, publishes them, reaps stuck runs, deletes old data.
* `worker-service`: consumes messages, claims the instance, makes the HTTP call, records the result.

### How reliability is achieved

| Concern | Mechanism |
|---|---|
| Several scheduler replicas | `SELECT ... FOR UPDATE SKIP LOCKED`: replicas never take the same row. No Redis lock. |
| Lost / duplicated messages | The `job_instances` row is a transactional outbox (`PENDING -> QUEUED`). Publisher confirms before marking `QUEUED`. |
| Duplicate deliveries | Worker first runs an atomic `PENDING/QUEUED -> RUNNING` UPDATE; only the winner executes. |
| Retries | Exponential backoff stored as a future-dated `PENDING` row, so retries survive restarts. |
| Crashed worker | Reaper fails `RUNNING` rows past `timeout + grace` (which triggers a retry) and re-queues stale `QUEUED` rows. |
| Missed runs while down | Fire once, then jump to the next future slot (no backlog replay). |
| Dead endpoint | Per-host circuit breaker **defers** work instead of dropping it. Poison messages go to a DLQ. |
| Counters | Atomic SQL increments (no read-modify-write); optimistic locking on `Job`. |
| Time | UTC `TIMESTAMPTZ` everywhere; cron evaluated in the job's time zone. |

### Security

* Passwords: bcrypt. Self-registration is always `ROLE_USER`; an admin exists only if `ADMIN_USERNAME/ADMIN_PASSWORD` are set.
* JWT: secret from `JWT_SECRET` (no default, min 32 chars), 60 min expiry, revocable via `POST /api/auth/logout-all`.
* Every job/instance/log endpoint is scoped to the authenticated user (other users' ids return 404).
* SSRF guard: only http(s); loopback, private, link-local (cloud metadata) and CGNAT targets are refused at creation **and** before each call; redirects are not followed; response bodies capped at 64 KB.
* Actuator beyond `/health` needs ADMIN. Swagger is off unless `SWAGGER_ENABLED=true`.

## Run it

```bash
cp .env.example .env     # set DB_PASSWORD, RABBIT_PASSWORD, JWT_SECRET (openssl rand -base64 48)
docker compose up --build
cd ui && npm ci && npm run dev    # http://localhost:5173
```

Local dev tip: to schedule calls to `localhost`, set `ALLOW_PRIVATE_TARGETS=true`.
Upgrading an old dev DB: `docker compose down -v` (the schema is now Flyway-owned and changed).

Build and test: `mvn clean verify`.

## API (all under `/api`, Bearer token)

`POST /auth/register|login` | `GET|POST /jobs` | `GET|PUT|DELETE /jobs/{id}` | `POST /jobs/{id}/trigger|pause|resume` |
`GET /jobs/{id}/instances` | `POST /jobs/instances/{id}/retry` | `GET /instances/{id}` | `GET /instances/job/{jobId}` |
`GET /logs`, `GET /logs/instance/{id}` | `GET /statistics/job/{id}` | `GET /analytics/status-counts`

Job schedule: `scheduleType` = `CRON` (5 or 6 fields), `INTERVAL` (`30s`, `5m`, `2h`, `1d`, min 10s) or `ONCE` (ISO instant) in `cronExpression`, plus `timezone`.

## Known limitations (honest list)

* Not compiled/tested against a real broker in CI yet beyond unit tests; add Testcontainers integration tests next.
* No login rate-limiting; put the API behind a gateway/WAF for that.
* DNS-rebinding between the SSRF check and the HTTP call is only narrowed, not eliminated (would need a pinned-IP connector).
* Circuit-breaker state is per worker process.
* Single RabbitMQ node and single Postgres; no HA setup included.
