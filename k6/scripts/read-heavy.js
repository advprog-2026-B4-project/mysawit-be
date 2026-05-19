/**
 * Skenario 2 — Read-Heavy (Dashboard Monitoring)
 *
 * Simulates 100 concurrent users reading dashboards: panen history,
 * wallet balance, and notifications. Redis cache should serve most
 * of these — p95 target is tighter than write-heavy (300ms).
 *
 * SLA: p95 < 300ms | 5xx error rate < 1%
 *
 * Run: k6 run --out json=k6/results/read-heavy.json k6/scripts/read-heavy.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const serverErrors = new Rate('server_errors');

const allUsers = new SharedArray('users', function () {
  const lines = open('../data/test_data.csv').split('\n');
  return lines
    .slice(1)
    .filter(l => l.trim() !== '')
    .map(l => {
      const [userId, role, token, kebunId, mandorId] = l.split(',');
      return { userId, role, token, kebunId, mandorId };
    });
});

export const options = {
  scenarios: {
    dashboard_read: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '1m', target: 0   },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<300'],
    server_errors:     ['rate<0.01'],
  },
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const user = allUsers[(__VU - 1) % allUsers.length];
  const auth = { headers: { 'Authorization': `Bearer ${user.token}` } };

  // Each iteration randomises which read endpoints to hit to simulate
  // realistic user navigation across the dashboard
  const scenario = Math.floor(Math.random() * 3);

  if (scenario === 0) {
    group('panen history', () => {
      const endpoint = user.role === 'BURUH'
        ? '/api/panen/buruh'
        : user.role === 'MANDOR'
          ? '/api/panen/mandor'
          : '/api/panen/buruh';

      const res = http.get(
        `${BASE_URL}${endpoint}`,
        { ...auth, tags: { endpoint } }
      );
      check(res, { 'panen list 200': r => r.status === 200 });
      serverErrors.add(res.status >= 500 ? 1 : 0);
    });
  } else if (scenario === 1) {
    group('wallet balance', () => {
      const res = http.get(
        `${BASE_URL}/api/pembayaran/wallet/${user.userId}`,
        { ...auth, tags: { endpoint: 'GET /api/pembayaran/wallet/:id' } }
      );
      check(res, { 'wallet 200': r => r.status === 200 });
      serverErrors.add(res.status >= 500 ? 1 : 0);
    });
  } else {
    group('notifications', () => {
      const res = http.get(
        `${BASE_URL}/api/notifications`,
        { ...auth, tags: { endpoint: 'GET /api/notifications' } }
      );
      check(res, { 'notifications 200': r => r.status === 200 });
      serverErrors.add(res.status >= 500 ? 1 : 0);
    });
  }

  sleep(0.5 + Math.random() * 1.5); // 0.5–2s think time
}

export function handleSummary(data) {
  return {
    'k6/results/read-heavy-summary.json': JSON.stringify(data, null, 2),
  };
}
