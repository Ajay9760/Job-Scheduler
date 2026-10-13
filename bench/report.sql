-- Results of the most recent bench run (instances of jobs created_by='bench').
WITH i AS (
  SELECT i.* FROM job_instances i JOIN jobs j ON j.id = i.job_id WHERE j.created_by = 'bench'
)
SELECT
  count(*)                                                        AS instances,
  count(*) FILTER (WHERE status = 'SUCCESS')                      AS success,
  count(*) FILTER (WHERE status NOT IN ('SUCCESS','PENDING','QUEUED','RUNNING')) AS failed,
  round(extract(epoch FROM max(completed_at) - min(scheduled_time))::numeric, 2) AS wall_seconds,
  round((count(*) FILTER (WHERE status='SUCCESS') /
         greatest(extract(epoch FROM max(completed_at) - min(scheduled_time)), 0.001))::numeric, 1) AS throughput_per_sec,
  count(DISTINCT worker_id)                                       AS workers_used,
  round((percentile_cont(0.50) WITHIN GROUP (ORDER BY extract(epoch FROM started_at - scheduled_time)*1000))::numeric)   AS queue_wait_p50_ms,
  round((percentile_cont(0.95) WITHIN GROUP (ORDER BY extract(epoch FROM started_at - scheduled_time)*1000))::numeric)   AS queue_wait_p95_ms,
  round((percentile_cont(0.99) WITHIN GROUP (ORDER BY extract(epoch FROM started_at - scheduled_time)*1000))::numeric)   AS queue_wait_p99_ms,
  round((percentile_cont(0.50) WITHIN GROUP (ORDER BY duration_ms))::numeric)  AS exec_p50_ms,
  round((percentile_cont(0.95) WITHIN GROUP (ORDER BY duration_ms))::numeric)  AS exec_p95_ms
FROM i WHERE completed_at IS NOT NULL;

-- Exactly-once check: every instance executed once, none duplicated or lost.
SELECT 'duplicate_runs' AS check, count(*) AS bad FROM (
  SELECT job_id, scheduled_time, retry_count FROM job_instances GROUP BY 1,2,3 HAVING count(*) > 1) d
UNION ALL
SELECT 'stuck_not_terminal', count(*) FROM job_instances i JOIN jobs j ON j.id=i.job_id
 WHERE j.created_by='bench' AND i.status IN ('PENDING','QUEUED','RUNNING')
UNION ALL
SELECT 'job_counter_mismatch', count(*) FROM jobs j WHERE j.created_by='bench'
 AND j.total_runs <> (SELECT count(*) FROM job_instances i WHERE i.job_id=j.id AND i.completed_at IS NOT NULL);
