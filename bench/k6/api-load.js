// API load test.  Run (compose bench overlay must be up):
//   docker run --rm -i --network host -e BASE=http://localhost:8080 grafana/k6 run - < bench/k6/api-load.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080';
const H = { 'Content-Type': 'application/json' };

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '60s', target: 200 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{op:create}': ['p(95)<500'],
    'http_req_duration{op:list}': ['p(95)<300'],
  },
};

export function setup() {
  const user = `k6_${Date.now()}`;
  const r = http.post(`${BASE}/api/auth/register`,
    JSON.stringify({ username: user, password: 'k6-password-123' }), { headers: H });
  check(r, { registered: (x) => x.status === 201 });
  return { token: r.json('token') };
}

export default function (d) {
  const auth = { headers: { ...H, Authorization: `Bearer ${d.token}` } };
  const body = JSON.stringify({
    name: `k6-${__VU}-${__ITER}`, targetUrl: 'http://target/', httpMethod: 'GET',
    scheduleType: 'CRON', cronExpression: '0 0 * * *', timezone: 'UTC',
  });
  const c = http.post(`${BASE}/api/jobs`, body, { ...auth, tags: { op: 'create' } });
  check(c, { created: (x) => x.status === 201 });
  const l = http.get(`${BASE}/api/jobs?page=0&size=20`, { ...auth, tags: { op: 'list' } });
  check(l, { listed: (x) => x.status === 200 });
  if (c.status === 201) {
    http.get(`${BASE}/api/jobs/${c.json('id')}`, { ...auth, tags: { op: 'get' } });
    http.post(`${BASE}/api/jobs/${c.json('id')}/trigger`, null, { ...auth, tags: { op: 'trigger' } });
  }
  sleep(0.2);
}
