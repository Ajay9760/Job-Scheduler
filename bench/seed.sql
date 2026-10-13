-- psql -v n=50000 -f seed.sql   : N instances, all due NOW, spread over 1000 jobs, target = local nginx.
INSERT INTO jobs (external_id, name, target_url, http_method, schedule_type, cron_expression, status, created_by, next_run_at)
SELECT 'bench-' || g || '-' || extract(epoch from clock_timestamp())::bigint, 'bench-' || g, 'http://target/', 'GET',
       'CRON', '0 0 0 1 1 *', 'PAUSED', 'bench', NULL
FROM generate_series(1, 1000) g;

WITH ids AS (SELECT array_agg(id ORDER BY id) AS a FROM jobs WHERE created_by = 'bench' AND name LIKE 'bench-%')
INSERT INTO job_instances (job_id, status, scheduled_time, retry_count)
SELECT ids.a[(g % 1000) + 1], 'PENDING', clock_timestamp() - (g * interval '1 microsecond'), 0
FROM ids, generate_series(0, :n - 1) g;
