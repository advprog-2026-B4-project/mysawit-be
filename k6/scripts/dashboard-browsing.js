/**
 * Scenario 2 — Dashboard Browsing (Role-Specific Multi-Page Sessions)
 *
 * Simulates 100 concurrent users from all roles browsing their respective
 * dashboards. Each user performs a realistic multi-page navigation session
 * based on their role, with think time between pages.
 *
 * ┌─────────┬────────────────────────────────────────────────────┐
 * │ Role    │ Session flow                                       │
 * ├─────────┼────────────────────────────────────────────────────┤
 * │ BURUH   │ panen history → wallet → notifications → payroll   │
 * │ MANDOR  │ pending panen → assignable panen → active delivery │
 * │         │ → recommendations → assigned supir → notifications │
 * │ SUPIR   │ active deliveries → delivery detail → update status│
 * │         │ → wallet → notifications                           │
 * ├─────────┴────────────────────────────────────────────────────┤
 *
 * Redis cache should serve most reads after first pass.
 *
 * SLA: p95 < 500ms | 5xx error rate < 1%
 *
 * Run: k6 run --out json=k6/results/dashboard-browsing.json k6/scripts/dashboard-browsing.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const IS_DRY_RUN = (__ENV.DRY_RUN || '').toLowerCase() === '1' || (__ENV.DRY_RUN || '').toLowerCase() === 'true';

const serverErrors    = new Rate('server_errors');
const pageViews       = new Counter('page_views');
const sessionDuration = new Trend('session_duration_ms');

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

export const options = IS_DRY_RUN
  ? {
      scenarios: {
        dashboard_browse: {
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
        dashboard_browse: {
          executor: 'ramping-vus',
          startVUs: 0,
          stages: [
            { duration: '1m', target: 100 },
            { duration: '8m', target: 100 },
            { duration: '1m', target: 0 },
          ],
          gracefulRampDown: '30s',
        },
      },
      thresholds: {
        http_req_duration: ['p(95)<500'],
        server_errors: ['rate<0.01'],
      },
      summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
    };

function auth(user) {
  return { headers: { 'Authorization': `Bearer ${user.token}` } };
}

function doGet(path, user, stepName, checkName) {
  const res = http.get(`${BASE_URL}${path}`, {
    ...auth(user),
    tags: { step: stepName },
  });
  check(res, { [checkName]: r => r.status === 200 });
  if (res.status >= 500) serverErrors.add(1);
  pageViews.add(1);
  return res;
}

// ── BURUH dashboard session ─────────────────────────────────────
function buruhSession(user) {
  const today = new Date().toISOString().split('T')[0];

  group('01-panen-history', () => {
    doGet(`/api/panen/buruh?startDate=${today}`, user, 'buruh_panen_history', 'history 200');
    sleep(0.5 + Math.random() * 1);
  });

  group('02-wallet', () => {
    doGet(`/api/pembayaran/wallet/${user.userId}`, user, 'buruh_wallet', 'wallet 200');
    sleep(0.3 + Math.random() * 0.7);
  });

  group('03-notifications', () => {
    doGet('/api/notifications', user, 'buruh_notifications', 'notif 200');
    sleep(0.3 + Math.random() * 0.7);
  });

  group('04-payroll', () => {
    doGet(`/api/pembayaran/payroll/user/${user.userId}?page=0&size=5`, user, 'buruh_payroll', 'payroll 200');
  });
}

// ── MANDOR dashboard session ────────────────────────────────────
function mandorSession(user) {
  group('01-pending-panen', () => {
    doGet('/api/panen/mandor', user, 'mandor_pending_panen', 'pending 200');
    sleep(0.5 + Math.random() * 1.5);
  });

  group('02-assignable-panen', () => {
    doGet('/api/pengiriman/mandor/panen', user, 'mandor_assignable', 'assignable 200');
    sleep(0.3 + Math.random() * 1);
  });

  group('03-active-deliveries', () => {
    doGet('/api/pengiriman/mandor/active', user, 'mandor_active_del', 'active 200');
    sleep(0.3 + Math.random() * 1);
  });

  // 30% chance: also hit the knapsack recommendation (more expensive)
  if (Math.random() < 0.3) {
    group('04-recommendation', () => {
      doGet('/api/pengiriman/mandor/recommendation?maxCapacity=400', user, 'mandor_recommend', 'recommend 200');
      sleep(0.5);
    });
  }

  group('05-assigned-supir', () => {
    doGet('/api/pengiriman/mandor/supir', user, 'mandor_supir', 'supir 200');
    sleep(0.3 + Math.random() * 0.7);
  });

  group('06-notifications', () => {
    doGet('/api/notifications', user, 'mandor_notifications', 'notif 200');
  });
}

// ── SUPIR dashboard session ─────────────────────────────────────
function supirSession(user) {
  group('01-my-deliveries', () => {
    const res = doGet('/api/pengiriman/supir', user, 'supir_deliveries', 'deliveries 200');
    sleep(0.5 + Math.random() * 1.5);

    // If deliveries exist, view detail of the first one
    try {
      const body = JSON.parse(res.body);
      const list = Array.isArray(body) ? body : (body.data || []);
      if (list.length > 0) {
        const deliveryId = list[0].pengirimanId || list[0].id;
        doGet(`/api/pengiriman/${deliveryId}`, user, 'supir_delivery_detail', 'detail 200');
      }
    } catch (_) { /* skip */ }
  });

  group('02-wallet', () => {
    doGet(`/api/pembayaran/wallet/${user.userId}`, user, 'supir_wallet', 'wallet 200');
    sleep(0.3 + Math.random() * 0.7);
  });

  group('03-notifications', () => {
    doGet('/api/notifications', user, 'supir_notifications', 'notif 200');
  });
}

// ── Dispatch by role ────────────────────────────────────────────
export default function () {
  const sessionStart = Date.now();
  const user = allUsers[(__VU - 1) % allUsers.length];

  switch (user.role) {
    case 'BURUH':
      buruhSession(user);
      break;
    case 'MANDOR':
      mandorSession(user);
      break;
    case 'SUPIR':
      supirSession(user);
      break;
    default:
      // Unknown role — just fetch notifications as a sanity check
      doGet('/api/notifications', user, 'unknown_notifications', 'notif 200');
  }

  sessionDuration.add(Date.now() - sessionStart);

  // Think time between sessions: 2–5s (switching between dashboard sections)
  sleep(2 + Math.random() * 3);
}

export function handleSummary(data) {
  return {
    'k6/results/dashboard-browsing-summary.json': JSON.stringify(data, null, 2),
  };
}
