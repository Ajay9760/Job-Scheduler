-- =====================================================================
-- Chronos baseline schema.
--
-- This is the ONLY place where the schema is defined. Every service runs these
-- migrations on start-up (Flyway serialises concurrent runs with a DB lock) and
-- Hibernate is configured with ddl-auto=validate, so entity/schema drift fails
-- fast instead of silently "healing" in one service and not in another.
--
-- All timestamps are TIMESTAMPTZ (stored in UTC). Do not use TIMESTAMP.
--
-- Upgrading an old dev database created by ddl-auto=update: drop and recreate it
-- (docker compose down -v). Column types and status values changed, so there is
-- no safe in-place path for dev data.
-- =====================================================================

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(100)  NOT NULL,
    password_hash  VARCHAR(200)  NOT NULL,
    roles          VARCHAR(200)  NOT NULL,
    enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    token_version  INTEGER       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE jobs (
    id                   BIGSERIAL PRIMARY KEY,
    version              BIGINT        NOT NULL DEFAULT 0,
    external_id          VARCHAR(64)   NOT NULL,
    name                 VARCHAR(255)  NOT NULL,
    description          VARCHAR(1000),
    target_url           VARCHAR(2000) NOT NULL,
    http_method          VARCHAR(10)   NOT NULL,
    request_body         TEXT,
    request_headers      JSONB,

    -- schedule_type: CRON | INTERVAL | ONCE.
    -- cron_expression holds the schedule expression for the chosen type:
    --   CRON     -> 5- or 6-field cron expression
    --   INTERVAL -> e.g. 30s, 5m, 2h, 1d
    --   ONCE     -> ISO-8601 instant, e.g. 2030-01-01T10:00:00Z
    schedule_type        VARCHAR(20)   NOT NULL,
    cron_expression      VARCHAR(255)  NOT NULL,
    timezone             VARCHAR(64)   NOT NULL DEFAULT 'UTC',

    priority             INTEGER       NOT NULL DEFAULT 5,
    timeout_seconds      INTEGER       NOT NULL DEFAULT 30,
    max_retries          INTEGER       NOT NULL DEFAULT 3,
    backoff_seconds      BIGINT        NOT NULL DEFAULT 30,
    webhook_url          VARCHAR(2000),

    status               VARCHAR(20)   NOT NULL,
    created_by           VARCHAR(100)  NOT NULL,

    next_run_at          TIMESTAMPTZ,
    last_run_at          TIMESTAMPTZ,
    last_success_at      TIMESTAMPTZ,
    last_failure_at      TIMESTAMPTZ,

    total_runs           BIGINT        NOT NULL DEFAULT 0,
    successful_runs      BIGINT        NOT NULL DEFAULT 0,
    failed_runs          BIGINT        NOT NULL DEFAULT 0,
    consecutive_failures INTEGER       NOT NULL DEFAULT 0,
    avg_duration_ms      BIGINT        NOT NULL DEFAULT 0,
    last_error           VARCHAR(2000),

    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_jobs_external_id   UNIQUE (external_id),
    CONSTRAINT ck_jobs_priority      CHECK (priority BETWEEN 1 AND 10),
    CONSTRAINT ck_jobs_timeout       CHECK (timeout_seconds BETWEEN 1 AND 300),
    CONSTRAINT ck_jobs_max_retries   CHECK (max_retries BETWEEN 0 AND 10),
    CONSTRAINT ck_jobs_backoff       CHECK (backoff_seconds BETWEEN 1 AND 3600),
    CONSTRAINT ck_jobs_status        CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_jobs_schedule_type CHECK (schedule_type IN ('CRON', 'INTERVAL', 'ONCE'))
);

-- The scheduler's hot query: "ACTIVE jobs whose next_run_at has passed".
CREATE INDEX idx_jobs_due        ON jobs (next_run_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_jobs_created_by ON jobs (created_by);
CREATE INDEX idx_jobs_status     ON jobs (status);

CREATE TABLE job_instances (
    id               BIGSERIAL PRIMARY KEY,
    job_id           BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,

    -- PENDING  : created, waiting to be published to the broker (this row is the outbox)
    -- QUEUED   : published to the broker, waiting for a worker
    -- RUNNING  : claimed by a worker
    -- terminal : SUCCESS | FAILED | TIMEOUT | CANCELLED | SKIPPED
    status           VARCHAR(20)  NOT NULL,

    scheduled_time   TIMESTAMPTZ  NOT NULL,
    dispatched_at    TIMESTAMPTZ,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    duration_ms      BIGINT,
    http_status_code INTEGER,
    response_body    TEXT,
    error_message    TEXT,
    retry_count      INTEGER      NOT NULL DEFAULT 0,
    worker_id        VARCHAR(120),

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_job_instances_status CHECK (status IN
        ('PENDING', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'TIMEOUT', 'CANCELLED', 'SKIPPED')),

    -- One row per (job, planned slot, attempt). Makes "create the instance for this
    -- cron slot" idempotent even if two schedulers ever raced.
    CONSTRAINT uq_job_instances_slot UNIQUE (job_id, scheduled_time, retry_count)
);

CREATE INDEX idx_job_instances_pending ON job_instances (scheduled_time) WHERE status = 'PENDING';
CREATE INDEX idx_job_instances_queued  ON job_instances (dispatched_at)  WHERE status = 'QUEUED';
CREATE INDEX idx_job_instances_running ON job_instances (started_at)     WHERE status = 'RUNNING';
CREATE INDEX idx_job_instances_job     ON job_instances (job_id, scheduled_time DESC);
CREATE INDEX idx_job_instances_status  ON job_instances (status, created_at DESC);
CREATE INDEX idx_job_instances_created ON job_instances (created_at);

CREATE TABLE job_logs (
    id          BIGSERIAL PRIMARY KEY,
    instance_id BIGINT      NOT NULL REFERENCES job_instances (id) ON DELETE CASCADE,
    job_id      BIGINT      NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    log_level   VARCHAR(10) NOT NULL,
    message     TEXT        NOT NULL,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_job_logs_level CHECK (log_level IN ('DEBUG', 'INFO', 'WARN', 'ERROR'))
);

CREATE INDEX idx_job_logs_instance ON job_logs (instance_id, created_at);
CREATE INDEX idx_job_logs_job      ON job_logs (job_id, created_at DESC);
CREATE INDEX idx_job_logs_created  ON job_logs (created_at);
