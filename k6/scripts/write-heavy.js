/**
 * Skenario 1 — Write-Heavy (Peak Hour Pagi)
 *
 * Simulates 500 buruh simultaneously submitting harvest reports at morning
 * rush hour. The UNIQUE(buruh_id, harvest_date) constraint means each buruh
 * gets one successful 201 per day; subsequent calls return 409 (expected).
 * Both paths exercise the write stack (auth filter, JPA, HikariCP).
 *
 * Infrastructure note: Tomcat max=100 threads + HikariCP max=50 conns means
 * 500 VU will queue. If p95 exceeds threshold, the bottleneck is server config
 * (not business logic). Tune tomcat.threads.max and hikari.maximum-pool-size.
 *
 * SLA: p95 < 1000ms | 5xx error rate < 1%
 *
 * Run: k6 run --out json=k6/results/write-heavy.json k6/scripts/write-heavy.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Custom metrics
const serverErrors   = new Rate('server_errors');
const successWrites  = new Counter('panen_created_201');
const conflictWrites = new Counter('panen_conflict_409');

const buruhUsers = new SharedArray('buruh', function () {
  const lines = open('../data/test_data.csv').split('\n');
  return lines
    .slice(1)
    .filter(l => l.trim() !== '')
    .map(l => {
      const [userId, role, token, kebunId, mandorId] = l.split(',');
      return { userId, role, token, kebunId, mandorId };
    })
    .filter(u => u.role === 'BURUH');
});

export const options = {
  scenarios: {
    write_panen_peak: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 500 },  // ramp-up: JVM JIT warms up here
        { duration: '5m', target: 500 },  // plateau: collect primary metrics
        { duration: '1m', target: 0   },  // ramp-down
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_duration:          ['p(95)<1000'],
    server_errors:              ['rate<0.01'],
    http_req_duration: [{ threshold: 'p(95)<1000', abortOnFail: false }],
  },
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const user = buruhUsers[(__VU - 1) % buruhUsers.length];

  const payload = JSON.stringify({
    weight:      Math.floor(50 + Math.random() * 250),
    photoUrls:   ['https://r2-object-storage.sawitt.my.id/load-test-placeholder.jpg'],
    description: `Load test submission VU-${__VU} iter-${__ITER}`,
  });

  const res = http.post(
    `${BASE_URL}/api/panen`,
    payload,
    {
      headers: {
        'Content-Type':  'application/json',
        'Authorization': `Bearer ${user.token}`,
      },
      tags: { endpoint: 'POST /api/panen' },
    }
  );

  // 201 = created, 409 = duplicate for today (both expected)
  const is201 = res.status === 201;
  const is409 = res.status === 409;
  const is5xx = res.status >= 500;

  check(res, {
    'status is 201 or 409': () => is201 || is409,
    'no server error':      () => !is5xx,
  });

  if (is201) successWrites.add(1);
  if (is409) conflictWrites.add(1);
  serverErrors.add(is5xx ? 1 : 0);

  // Think time: 0–1s to avoid hammering too rhythmically
  sleep(Math.random());
}

export function handleSummary(data) {
  return {
    'k6/results/write-heavy-summary.json': JSON.stringify(data, null, 2),
  };
}
