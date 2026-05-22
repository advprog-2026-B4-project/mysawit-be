/**
 * Scenario 1 — Buruh Morning Rush (Realistic)
 *
 * Simulates 500 BURUH going through their morning workflow at plantation
 * start-of-day (06:00–08:00). Each BURUH performs a multi-step session:
 *
 *   1. Check if already submitted today     GET  /api/panen/checksubmission
 *   2. Get presigned URL for photo upload   GET  /api/storage/upload-token
 *   3. Submit harvest report with photo     POST /api/panen
 *   4. View own panen history today         GET  /api/panen/buruh?startDate=today
 *   5. Check unread notifications           GET  /api/notifications
 *   6. Check wallet balance                 GET  /api/pembayaran/wallet/:id
 *
 * Each BURUH submits at most once per test run (shared-iterations,
 * one iteration per BURUH). Steps 1–2 warm caches; step 3 is the
 * actual write; steps 4–6 verify the read path immediately after.
 *
 * SLA: p95 < 1500ms (multi-step), 5xx rate < 1%
 *
 * Run: k6 run --out json=k6/results/buruh-morning-rush.json k6/scripts/buruh-morning-rush.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const IS_DRY_RUN = (__ENV.DRY_RUN || '').toLowerCase() === '1' || (__ENV.DRY_RUN || '').toLowerCase() === 'true';

// Custom metrics
const serverErrors     = new Rate('server_errors');
const harvestCreated   = new Counter('harvest_created_201');
const harvestConflict  = new Counter('harvest_conflict_409');
const alreadySubmitted = new Counter('already_submitted_hits');
const stepFailures     = new Counter('step_failures');
const sessionDuration  = new Trend('session_duration_ms');

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

export const options = IS_DRY_RUN
    ? {
      scenarios: {
        buruh_morning: {
          executor: 'shared-iterations',
          vus: 1,
          iterations: Math.max(1, buruhUsers.length),
          maxDuration: '20s',
        },
      },
      thresholds: {},
      summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
    }
  : {
      scenarios: {
        buruh_morning: {
          executor: 'shared-iterations',
          vus: Math.min(200, Math.max(1, buruhUsers.length)),
          iterations: Math.max(1, buruhUsers.length),
          maxDuration: '10m',
        },
      },
      thresholds: {
        http_req_duration: ['p(95)<1500'],
        server_errors: ['rate<0.01'],
      },
      summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
    };

function authHeaders(user) {
  return { headers: { 'Authorization': `Bearer ${user.token}` } };
}

function jsonHeaders(user) {
  return {
    headers: {
      'Authorization': `Bearer ${user.token}`,
      'Content-Type': 'application/json',
    },
  };
}

export default function () {
  const sessionStart = Date.now();
  const idx = __ITER % buruhUsers.length;
  const user = buruhUsers[idx];

  let alreadyDone = false;

  // ── Step 1: Check if already submitted ─────────────────────────
  const checkRes = group('01-check-submission', () => {
    const res = http.get(
      `${BASE_URL}/api/panen/checksubmission`,
      { ...authHeaders(user), tags: { step: 'check_submission' } }
    );
    const ok = check(res, { 'check 200': r => r.status === 200 });
    if (!ok) { stepFailures.add(1); return res; }

    try {
      const body = JSON.parse(res.body);
      // Response shape: ApiResponse<Boolean> — data is boolean or wrapped
      const submitted = body.data === true || body === true;
      if (submitted) { alreadySubmitted.add(1); alreadyDone = true; }
    } catch (_) { /* non-critical parse failure, continue */ }

    return res;
  });
  if (checkRes.status >= 500) serverErrors.add(1);

  // ── Step 2: Get upload token (presigned URL) ───────────────────
  group('02-get-upload-token', () => {
    const res = http.get(
      `${BASE_URL}/api/storage/upload-token?contentType=image/jpeg`,
      { ...authHeaders(user), tags: { step: 'upload_token' } }
    );
    const ok = check(res, { 'upload-token ok': r => r.status === 200 || r.status === 401 || r.status === 403 });
    if (!ok) stepFailures.add(1);
    if (res.status >= 500) serverErrors.add(1);
  });

  // ── Step 3: Submit harvest report ──────────────────────────────
  // Skip if already submitted today (avoid artificial 409 flood)
  if (!alreadyDone) {
    group('03-submit-harvest', () => {
      const payload = JSON.stringify({
        weight: Math.floor(80 + Math.random() * 220),       // 80–300 kg
        photoUrls: ['https://r2-object-storage.sawitt.my.id/load-test-panen.jpg'],
        description: `Morning harvest — load test VU-${__VU}`,
      });

      const res = http.post(
        `${BASE_URL}/api/panen`,
        payload,
        { ...jsonHeaders(user), tags: { step: 'submit_harvest' } }
      );

      const is201 = res.status === 201;
      const is409 = res.status === 409;
      const is4xx = res.status >= 400 && res.status < 500;

      check(res, {
        'submit 201 or 409': () => is201 || is409,
        'no server error': () => res.status < 500,
      });

      if (is201) harvestCreated.add(1);
      if (is409) harvestConflict.add(1);
      if (!is201 && !is409) stepFailures.add(1);
      if (res.status >= 500) serverErrors.add(1);
    });
  }

  // ── Step 4: View today's panen history ─────────────────────────
  group('04-view-harvest-today', () => {
    const today = new Date().toISOString().split('T')[0];
    const res = http.get(
      `${BASE_URL}/api/panen/buruh?startDate=${today}`,
      { ...authHeaders(user), tags: { step: 'view_panen_today' } }
    );
    check(res, { 'history 200': r => r.status === 200 });
    if (res.status >= 500) serverErrors.add(1);
  });

  // ── Step 5: Check notifications ────────────────────────────────
  group('05-notifications', () => {
    const res = http.get(
      `${BASE_URL}/api/notifications`,
      { ...authHeaders(user), tags: { step: 'notifications' } }
    );
    check(res, { 'notif 200': r => r.status === 200 });
    if (res.status >= 500) serverErrors.add(1);

    // If there are unread notifications, mark the first one as read
    try {
      const notifications = JSON.parse(res.body);
      const data = Array.isArray(notifications) ? notifications : (notifications.data || []);
      const unread = data.find(n => !n.isRead);
      if (unread) {
        http.post(
          `${BASE_URL}/api/notifications/${unread.id || unread.notificationId}/read`,
          null,
          { ...authHeaders(user), tags: { step: 'mark_notif_read' } }
        );
      }
    } catch (_) { /* skip */ }
  });

  // ── Step 6: Check wallet balance ───────────────────────────────
  group('06-wallet-balance', () => {
    const res = http.get(
      `${BASE_URL}/api/pembayaran/wallet/${user.userId}`,
      { ...authHeaders(user), tags: { step: 'wallet_balance' } }
    );
    check(res, { 'wallet 200': r => r.status === 200 });
    if (res.status >= 500) serverErrors.add(1);
  });

  // ── Record session duration ────────────────────────────────────
  sessionDuration.add(Date.now() - sessionStart);

  // Realistic think time between actions: 1–3s (buruh checks phone between steps)
  sleep(1 + Math.random() * 2);
}

export function handleSummary(data) {
  return {
    'k6/results/buruh-morning-rush-summary.json': JSON.stringify(data, null, 2),
  };
}
