-- Migration: Add statistics and tracking fields to jobs table
ALTER TABLE jobs
ADD COLUMN IF NOT EXISTS total_runs INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS successful_runs INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS failed_runs INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_run_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_success_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_failure_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS next_run_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS avg_duration_ms BIGINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_error_message TEXT,
ADD COLUMN IF NOT EXISTS consecutive_failures INTEGER DEFAULT 0;

-- Create index for next_run_at for scheduler queries
CREATE INDEX IF NOT EXISTS idx_jobs_next_run_at ON jobs(next_run_at) WHERE status = 'ACTIVE';

-- Create index for performance monitoring
CREATE INDEX IF NOT EXISTS idx_jobs_last_run_at ON jobs(last_run_at DESC);

-- Update existing jobs with default next_run_at
UPDATE jobs
SET next_run_at = CURRENT_TIMESTAMP
WHERE status = 'ACTIVE' AND next_run_at IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN jobs.total_runs IS 'Total number of execution attempts';
COMMENT ON COLUMN jobs.successful_runs IS 'Number of successful executions';
COMMENT ON COLUMN jobs.failed_runs IS 'Number of failed executions';
COMMENT ON COLUMN jobs.next_run_at IS 'Calculated next scheduled execution time';
COMMENT ON COLUMN jobs.avg_duration_ms IS 'Average execution duration in milliseconds';
COMMENT ON COLUMN jobs.consecutive_failures IS 'Count of consecutive failures for circuit breaker';