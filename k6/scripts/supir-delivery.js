/**
 * Scenario 4 — Supir Delivery Lifecycle (Realistic)
 *
 * Simulates 30 SUPIR drivers managing their assigned deliveries throughout
 * the day. Each SUPIR follows the real delivery workflow:
 *
 *   1. Check assigned deliveries              GET  /api/pengiriman/supir
 *   2. Start a delivery (IN_TRANSIT)          PUT  /api/pengiriman/:id/status
 *   3. Complete a delivery (DELIVERED)        PUT  /api/pengiriman/:id/status
 *   4. Check wallet balance                   GET  /api/pembayaran/wallet/:id
 *   5. View notifications                     GET  /api/notifications
 *
 * The test focuses on the state-machine transitions of pengiriman:
 *   ASSIGNED → IN_TRANSIT → DELIVERED
 *
 * SLA: p95 < 800ms | 5xx error rate < 1%
 *
 * Run: k6 run --out json=k6/results/supir-delivery.json k6/scripts/supir-delivery.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const IS_DRY_RUN = (__ENV.DRY_RUN || '').toLowerCase() === '1' || (__ENV.DRY_RUN || '').toLowerCase() === 'true';

const serverErrors   = new Rate('server_errors');
const transitStarts  = new Counter('delivery_in_transit');
const deliveryDones  = new Counter('delivery_completed');
const emptyAssigns   = new Counter('empty_assignments');

const supirUsers = new SharedArray('supirs', function () {
  const lines = open('../data/test_data.csv').split('\n');
  return lines
    .slice(1)
    .filter(l => l.trim() !== '')
    .map(l => {
      const [userId, role, token, kebunId, mandorId] = l.split(',');
      return { userId, role, token, kebunId, mandorId };
    })
    .filter(u => u.role === 'SUPIR');
});

export const options = IS_DRY_RUN
  ? {
      scenarios: {
        supir_delivery: {
          executor: 'constant-vus',
          vus: 1,
          duration: '10s',
        },
      },
      thresholds: {},
      summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
    }
  : {
      scenarios: {
        supir_delivery: {
          executor: 'ramping-vus',
          startVUs: 0,
          stages: [
            { duration: '30s', target: 30 },
            { duration: '10m', target: 30 },
            { duration: '30s', target: 0 },
          ],
          gracefulRampDown: '30s',
        },
      },
      thresholds: {
        http_req_duration: ['p(95)<800'],
        server_errors: ['rate<0.01'],
      },
      summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
    };

function auth(user) {
  return { headers: { 'Authorization': `Bearer ${user.token}` } };
}

function jsonAuth(user) {
  return {
    headers: {
      'Authorization': `Bearer ${user.token}`,
      'Content-Type': 'application/json',
    },
  };
}

export default function () {
  const supir = supirUsers[(__VU - 1) % supirUsers.length];

  // ── Step 1: Fetch assigned deliveries ──────────────────────────
  let deliveries = [];
  group('01-fetch-deliveries', () => {
    const res = http.get(
      `${BASE_URL}/api/pengiriman/supir`,
      { ...auth(supir), tags: { step: 'fetch_deliveries' } }
    );

    if (res.status !== 200) {
      if (res.status >= 500) serverErrors.add(1);
      sleep(3);
      return;
    }

    try {
      const body = JSON.parse(res.body);
      deliveries = Array.isArray(body) ? body : (body.data || []);
    } catch (_) {
      sleep(2);
      return;
    }

    if (deliveries.length === 0) {
      emptyAssigns.add(1);
    }
  });

  if (deliveries.length === 0) {
    sleep(15); // Long backoff — nothing assigned
    return;
  }

  // ── Step 2: Process deliveries — transition states ─────────────
  // Look for ASSIGNED deliveries to start, or IN_TRANSIT to complete
  const assignedOnes = deliveries.filter(d => {
    const s = (d.status || '').toUpperCase().replace(' ', '_');
    return s === 'ASSIGNED' || s === 'PENDING_SUPIR';
  });

  const inTransitOnes = deliveries.filter(d => {
    const s = (d.status || '').toUpperCase().replace(' ', '_');
    return s === 'IN_TRANSIT';
  });

  // Prefer starting a new delivery over completing one
  // (more realistic: supir actively picks up new assignments)
  const hasAssigned = assignedOnes.length > 0;
  const hasInTransit = inTransitOnes.length > 0;

  if (hasAssigned) {
    group('02-start-delivery', () => {
      const target = assignedOnes[Math.floor(Math.random() * assignedOnes.length)];
      const deliveryId = target.pengirimanId || target.id;

      const payload = JSON.stringify({ newStatus: 'IN_TRANSIT' });
      const res = http.put(
        `${BASE_URL}/api/pengiriman/${deliveryId}/status`,
        payload,
        { ...jsonAuth(supir), tags: { step: 'start_delivery' } }
      );

      const ok = res.status === 200 || res.status === 204;
      check(res, {
        'start delivery ok': () => ok,
        'no server error': () => res.status < 500,
      });

      if (ok) transitStarts.add(1);
      if (res.status >= 500) serverErrors.add(1);
    });
  } else if (hasInTransit) {
    group('03-complete-delivery', () => {
      const target = inTransitOnes[Math.floor(Math.random() * inTransitOnes.length)];
      const deliveryId = target.pengirimanId || target.id;

      const payload = JSON.stringify({ newStatus: 'DELIVERED' });
      const res = http.put(
        `${BASE_URL}/api/pengiriman/${deliveryId}/status`,
        payload,
        { ...jsonAuth(supir), tags: { step: 'complete_delivery' } }
      );

      const ok = res.status === 200 || res.status === 204;
      check(res, {
        'complete delivery ok': () => ok,
        'no server error': () => res.status < 500,
      });

      if (ok) deliveryDones.add(1);
      if (res.status >= 500) serverErrors.add(1);
    });
  }

  // ── Step 4: Check wallet balance ───────────────────────────────
  group('04-wallet', () => {
    const res = http.get(
      `${BASE_URL}/api/pembayaran/wallet/${supir.userId}`,
      { ...auth(supir), tags: { step: 'wallet_balance' } }
    );
    check(res, { 'wallet 200': r => r.status === 200 });
    if (res.status >= 500) serverErrors.add(1);
  });

  // ── Step 5: Check notifications ────────────────────────────────
  group('05-notifications', () => {
    const res = http.get(
      `${BASE_URL}/api/notifications`,
      { ...auth(supir), tags: { step: 'notifications' } }
    );
    check(res, { 'notif 200': r => r.status === 200 });
    if (res.status >= 500) serverErrors.add(1);
  });

  // Realistic delivery cadence: 10–30s between deliveries
  // (driving between kebun locations)
  sleep(10 + Math.random() * 20);
}

export function handleSummary(data) {
  return {
    'k6/results/supir-delivery-summary.json': JSON.stringify(data, null, 2),
  };
}
