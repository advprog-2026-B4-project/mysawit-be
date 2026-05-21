/**
 * Scenario 5 — Mixed Workload: Real Plantation Day
 *
 * Simulates a full working day at the plantation with realistic role ratios:
 *   - 60% BURUH  (submit harvest, browse history, check wallet)
 *   - 20% MANDOR (review panen, manage deliveries, use knapsack)
 *   - 15% SUPIR  (check deliveries, update status, browse)
 *   -  5% ADMIN  (list users, list payrolls, browse kebun)
 *
 * Each VU picks a role proportional to real-world usage. The executor
 * runs constant VUs for an extended period to simulate steady-state load.
 *
 * This is the most realistic single test — run after seeding the DB.
 *
 * SLA: p95 < 1000ms | 5xx rate < 1%
 *
 * Run: k6 run --out json=k6/results/mixed-workload.json k6/scripts/mixed-workload.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const IS_DRY_RUN = (__ENV.DRY_RUN || '').toLowerCase() === '1' || (__ENV.DRY_RUN || '').toLowerCase() === 'true';

const serverErrors    = new Rate('server_errors');
const harvestSubmits  = new Counter('harvest_submits');
const approvals       = new Counter('panen_approvals');
const deliveryUpdates = new Counter('delivery_updates');
const sessionDuration = new Trend('session_duration_ms');

// ── User pools by role ──────────────────────────────────────────
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

const usersByRole = {
  BURUH:  allUsers.filter(u => u.role === 'BURUH'),
  MANDOR: allUsers.filter(u => u.role === 'MANDOR'),
  SUPIR:  allUsers.filter(u => u.role === 'SUPIR'),
  ADMIN:  allUsers.filter(u => u.role === 'ADMIN'),
};

// Role distribution: 60/20/15/5
const roleWeights = [
  { role: 'BURUH',  cumProb: 0.60 },
  { role: 'MANDOR', cumProb: 0.80 },
  { role: 'SUPIR',  cumProb: 0.95 },
  { role: 'ADMIN',  cumProb: 1.00 },
];

function pickRole() {
  const r = Math.random();
  for (const { role, cumProb } of roleWeights) {
    if (r < cumProb) return role;
  }
  return 'BURUH';
}

function pickUser(role) {
  const pool = usersByRole[role] || usersByRole.BURUH;
  return pool[Math.floor(Math.random() * pool.length)];
}

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

function safeGet(path, user, checkName, stepName) {
  const res = http.get(`${BASE_URL}${path}`, {
    ...auth(user),
    tags: { step: stepName },
  });
  check(res, { [checkName]: r => r.status === 200 });
  if (res.status >= 500) serverErrors.add(1);
  return res;
}

function safePost(path, body, user, checkName, stepName) {
  const res = http.post(`${BASE_URL}${path}`, body, {
    ...jsonAuth(user),
    tags: { step: stepName },
  });
  check(res, { [checkName]: r => r.status < 500 });
  if (res.status >= 500) serverErrors.add(1);
  return res;
}

function safePatch(path, body, user, checkName, stepName) {
  const res = http.patch(`${BASE_URL}${path}`, body, {
    ...jsonAuth(user),
    tags: { step: stepName },
  });
  check(res, { [checkName]: r => r.status < 500 });
  if (res.status >= 500) serverErrors.add(1);
  return res;
}

function safePut(path, body, user, checkName, stepName) {
  const res = http.put(`${BASE_URL}${path}`, body, {
    ...jsonAuth(user),
    tags: { step: stepName },
  });
  check(res, { [checkName]: r => r.status < 500 });
  if (res.status >= 500) serverErrors.add(1);
  return res;
}

// ── BURUH: submit harvest + browse ──────────────────────────────
function buruhFlow(user) {
  group('buruh-check-submission', () => {
    safeGet('/api/panen/checksubmission', user, 'check ok', 'check_submission');
  });

  // 40% chance: actually submit a new harvest report
  if (Math.random() < 0.4) {
    group('buruh-submit', () => {
      const payload = JSON.stringify({
        weight: Math.floor(50 + Math.random() * 250),
        photoUrls: ['https://r2-object-storage.sawitt.my.id/load-test-mixed.jpg'],
        description: `Mixed workload harvest VU-${__VU}`,
      });
      const res = safePost('/api/panen', payload, user, 'submit ok', 'submit_harvest');
      const is201 = res.status === 201;
      const is409 = res.status === 409;
      if (is201) harvestSubmits.add(1);
      // 409 is expected if already submitted, but counts as a write attempt
    });
  }

  group('buruh-browse', () => {
    const today = new Date().toISOString().split('T')[0];
    safeGet(`/api/panen/buruh?startDate=${today}`, user, 'history ok', 'panen_history');
    safeGet('/api/notifications', user, 'notif ok', 'notifications');
  });

  sleep(1 + Math.random() * 3);
}

// ── MANDOR: review + manage ─────────────────────────────────────
function mandorFlow(user) {
  group('mandor-pending', () => {
    const res = safeGet('/api/panen/mandor', user, 'pending ok', 'pending_panen');
    sleep(0.5);

    // If there are pending items, approve one
    try {
      const body = JSON.parse(res.body);
      const list = Array.isArray(body) ? body : (body.data || []);
      const pending = list.filter(p => {
        const s = (p.status || '').toUpperCase();
        return s === 'PENDING';
      });
      if (pending.length > 0) {
        const target = pending[0];
        const panenId = target.id || target.panenId;
        const reviewPayload = JSON.stringify({ action: 'APPROVE', rejectionReason: null });
        safePatch(`/api/panen/${panenId}/review`, reviewPayload, user, 'approval ok', 'approve_panen');
        approvals.add(1);
      }
    } catch (_) { /* skip */ }
  });

  // 25% chance: check knapsack recommendation
  if (Math.random() < 0.25) {
    group('mandor-knapsack', () => {
      safeGet('/api/pengiriman/mandor/recommendation', user, 'knapsack ok', 'knapsack_recommend');
    });
  }

  group('mandor-browse', () => {
    safeGet('/api/pengiriman/mandor/active', user, 'active ok', 'active_deliveries');
    safeGet('/api/notifications', user, 'notif ok', 'notifications');
  });

  sleep(2 + Math.random() * 4);
}

// ── SUPIR: deliver + update status ──────────────────────────────
function supirFlow(user) {
  group('supir-deliveries', () => {
    const res = safeGet('/api/pengiriman/supir', user, 'deliveries ok', 'supir_deliveries');
    sleep(0.5);

    // Update delivery status if there are active ones
    try {
      const body = JSON.parse(res.body);
      const list = Array.isArray(body) ? body : (body.data || []);
      const active = list.filter(d => {
        const s = (d.status || '').toUpperCase().replace(' ', '_');
        return s === 'ASSIGNED' || s === 'IN_TRANSIT';
      });
      if (active.length > 0) {
        const target = active[0];
        const deliveryId = target.pengirimanId || target.id;
        const status = (target.status || '').toUpperCase().replace(' ', '_');
        const newStatus = status === 'ASSIGNED' ? 'IN_TRANSIT' : 'DELIVERED';
        const payload = JSON.stringify({ newStatus });
        safePut(`/api/pengiriman/${deliveryId}/status`, payload, user, 'update ok', 'update_status');
        deliveryUpdates.add(1);
      }
    } catch (_) { /* skip */ }
  });

  group('supir-browse', () => {
    safeGet(`/api/pembayaran/wallet/${user.userId}`, user, 'wallet ok', 'wallet');
    safeGet('/api/notifications', user, 'notif ok', 'notifications');
  });

  sleep(5 + Math.random() * 10);
}

// ── ADMIN: monitor + manage ─────────────────────────────────────
function adminFlow(user) {
  group('admin-monitor', () => {
    safeGet('/api/pembayaran/payroll?page=0&size=5', user, 'payroll ok', 'list_payrolls');
    sleep(0.5);
    safeGet('/api/kebun', user, 'kebun ok', 'list_kebun');
  });

  // 30% chance: list users with filter
  if (Math.random() < 0.3) {
    safeGet('/api/users?role=BURUH', user, 'users ok', 'list_users');
  }

  sleep(3 + Math.random() * 5);
}

// ── Main ────────────────────────────────────────────────────────
export const options = IS_DRY_RUN
  ? {
      scenarios: {
        mixed_load: {
          executor: 'constant-vus',
          vus: 1,
          duration: '15s',
        },
      },
      thresholds: {},
      summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
    }
  : {
      scenarios: {
        mixed_load: {
          executor: 'ramping-vus',
          startVUs: 0,
          stages: [
            { duration: '2m',  target: 100 },
            { duration: '10m', target: 100 },
            { duration: '2m',  target: 200 },
            { duration: '10m', target: 200 },
            { duration: '2m',  target: 0 },
          ],
          gracefulRampDown: '1m',
        },
      },
      thresholds: {
        http_req_duration: ['p(95)<1000'],
        server_errors: ['rate<0.01'],
      },
      summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
    };

export default function () {
  const sessionStart = Date.now();
  const role = pickRole();
  const user = pickUser(role);

  switch (role) {
    case 'BURUH':  buruhFlow(user);  break;
    case 'MANDOR': mandorFlow(user); break;
    case 'SUPIR':  supirFlow(user);  break;
    case 'ADMIN':  adminFlow(user);  break;
  }

  sessionDuration.add(Date.now() - sessionStart);
}

export function handleSummary(data) {
  return {
    'k6/results/mixed-workload-summary.json': JSON.stringify(data, null, 2),
  };
}
