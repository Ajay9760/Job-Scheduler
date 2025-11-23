CREATE TABLE jobs (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    cron_expression VARCHAR(255) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    next_run_at     TIMESTAMP,
    last_run_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
