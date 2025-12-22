-- Migration: Create job_instances table for tracking job executions
CREATE TABLE IF NOT EXISTS job_instances (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    scheduled_time TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    http_status_code INTEGER,
    response_body TEXT,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_job_instances_job_id ON job_instances(job_id);
CREATE INDEX idx_job_instances_status ON job_instances(status);
CREATE INDEX idx_job_instances_scheduled_time ON job_instances(scheduled_time);
CREATE INDEX idx_job_instances_created_at ON job_instances(created_at DESC);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_job_instances_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_job_instances_updated_at
    BEFORE UPDATE ON job_instances
    FOR EACH ROW
    EXECUTE FUNCTION update_job_instances_updated_at();