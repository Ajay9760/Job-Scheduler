# .\bench\run.ps1 [-N 50000] [-Schedulers 2] [-Workers 4]    (needs a .env, see .env.example)
param([int]$N = 50000, [int]$Schedulers = 2, [int]$Workers = 4)
$ErrorActionPreference = "Stop"
$dc = @("compose", "-f", "docker-compose.yml", "-f", "docker-compose.bench.yml")
function Psql { param([string[]]$a, [string]$file)
  if ($file) { Get-Content $file -Raw | docker @dc exec -T postgres psql -U chronos -d chronos -v ON_ERROR_STOP=1 @a }
  else { docker @dc exec -T postgres psql -U chronos -d chronos -v ON_ERROR_STOP=1 @a } }
docker @dc up -d --build --scale scheduler=$Schedulers --scale worker=$Workers
do { Start-Sleep 2; $ok = (Psql @("-At","-c","select 1 from jobs limit 1") 2>$null) } until ($LASTEXITCODE -eq 0)
Psql @("-c","DELETE FROM jobs WHERE created_by='bench'") | Out-Null
Psql @("-v","n=$N") "bench/seed.sql"
Write-Host "seeded $N due instances; waiting for drain (schedulers=$Schedulers workers=$Workers)..."
do {
  $left = [int](Psql @("-At","-c","select count(*) from job_instances i join jobs j on j.id=i.job_id where j.created_by='bench' and i.status in ('PENDING','QUEUED','RUNNING')"))
  Write-Host "remaining: $left"; if ($left -gt 0) { Start-Sleep 3 }
} while ($left -gt 0)
Psql @("-x") "bench/report.sql"
