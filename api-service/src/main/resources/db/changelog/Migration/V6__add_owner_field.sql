-- Migration: Add owner field to jobs table for multi-tenancy
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS owner VARCHAR(100);

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_jobs_owner ON jobs(owner);

-- Update existing jobs to have a default owner (optional)
UPDATE jobs SET owner = 'system' WHERE owner IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN jobs.owner IS 'User/tenant who owns this job';