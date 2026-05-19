/**
 * Skenario 3 — Async Event-Driven (Bulk Approval)
 *
 * 20 mandor VUs approve PENDING harvest reports continuously for 30 minutes.
 * Each approval triggers async payroll creation in the background (Spring events).
 *
 * This test is NOT about latency spikes — it's about SUSTAINED load to detect:
 *   - Memory leak from uncollected event listeners (watch jvm_memory_used_bytes in Grafana)
 *   - ThreadPool exhaustion from async event queue buildup
 *   - HikariCP connection leak
 *
 * HOW TO MONITOR:
 *   Open Grafana at http://localhost:3002 before running.
 *   Watch panel: JVM Heap Used (jvm_memory_used_bytes{area="heap"})
 *   A steadily rising heap that never plateaus over 30 min indicates a leak.
 *
 * SLA:
 *   - p95 < 500ms (approval endpoint itself)
 *   - 5xx error rate < 1%
 *   - JVM heap must not grow monotonically over the 30-minute run
 *     (verified manually in Grafana — k6 cannot measure this)
 *
 * Run: k6 run --out json=k6/results/async-approval.json k6/scripts/async-approval.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const serverErrors   = new Rate('server_errors');
const approvedTotal  = new Counter('panen_approved_total');
const emptyQueueHits = new Counter('empty_queue_hits');

const mandorUsers = new SharedArray('mandors', function () {
  const lines = open('../data/test_data.csv').split('\n');
  return lines
    .slice(1)
    .filter(l => l.trim() !== '')
    .map(l => {
      const [userId, role, token, kebunId, mandorId] = l.split(',');
      return { userId, role, token, kebunId, mandorId };
    })
    .filter(u => u.role === 'MANDOR');
});

export const options = {
  scenarios: {
    async_bulk_approval: {
      executor: 'constant-vus',
      vus:      20,
      duration: '30m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    server_errors:     ['rate<0.01'],
  },
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const mandor = mandorUsers[(__VU - 1) % mandorUsers.length];
  const auth   = { headers: { 'Authorization': `Bearer ${mandor.token}` } };

  // Step 1: Fetch pending panen for this mandor
  const listRes = http.get(
    `${BASE_URL}/api/panen/mandor`,
    { ...auth, tags: { endpoint: 'GET /api/panen/mandor' } }
  );

  if (listRes.status !== 200) {
    serverErrors.add(1);
    sleep(2);
    return;
  }

  let pendingList = [];
  try {
    const body = JSON.parse(listRes.body);
    // Handle both direct array and wrapped response shapes
    pendingList = Array.isArray(body) ? body : (body.data || body.content || []);
    pendingList = pendingList.filter(p => p.status === 'PENDING');
  } catch (_) {
    sleep(1);
    return;
  }

  if (pendingList.length === 0) {
    emptyQueueHits.add(1);
    sleep(5); // Back off when queue is empty
    return;
  }

  // Step 2: Approve the first pending item in the list
  const target = pendingList[0];
  const panenId = target.id || target.panenId;

  const approveRes = http.patch(
    `${BASE_URL}/api/panen/${panenId}/review`,
    JSON.stringify({ action: 'APPROVE', rejectionReason: null }),
    {
      ...auth,
      headers: {
        ...auth.headers,
        'Content-Type': 'application/json',
      },
      tags: { endpoint: 'PATCH /api/panen/:id/review' },
    }
  );

  const ok = approveRes.status === 200 || approveRes.status === 204;

  check(approveRes, {
    'approval succeeded': () => ok,
    'no server error':    () => approveRes.status < 500,
  });

  if (ok) approvedTotal.add(1);
  serverErrors.add(approveRes.status >= 500 ? 1 : 0);

  // 1–3s pause: realistic mandor review cadence, also prevents
  // hammering DB with approval events faster than async handlers process them
  sleep(1 + Math.random() * 2);
}

export function handleSummary(data) {
  return {
    'k6/results/async-approval-summary.json': JSON.stringify(data, null, 2),
  };
}
