/**
 * Scenario 3 — Mandor Review & Approval (Realistic)
 *
 * Simulates 20 MANDOR reviewing and approving/rejecting PENDING harvest
 * reports over a 30-minute period. Each approval triggers async payroll
 * creation + notification via Spring domain events.
 *
 * Realistic review flow:
 *   1. Fetch pending panen list              GET  /api/panen/mandor
 *   2. Pick one item (random, first, or by weight)
 *   3. Approve (80%) or Reject (20%)         PATCH /api/panen/:id/review
 *   4. Mark a notification as read           POST /api/notifications/:id/read
 *   5. Check active pengiriman periodically  GET  /api/pengiriman/mandor/active
 *
 * WHAT TO MONITOR (Grafana):
 *   - JVM heap: jvm_memory_used_bytes{area="heap"} — must plateau, not grow
 *   - HikariCP active connections: should stay well below max-pool-size
 *   - Thread pool: async executor queue depth
 *
 * SLA:
 *   - p95 < 500ms (individual requests)
 *   - 5xx error rate < 1%
 *   - No monotonic JVM heap growth over 30 min
 *
 * Run: k6 run --out json=k6/results/mandor-review.json k6/scripts/mandor-review.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const IS_DRY_RUN = (__ENV.DRY_RUN || '').toLowerCase() === '1' || (__ENV.DRY_RUN || '').toLowerCase() === 'true';

const serverErrors  = new Rate('server_errors');
const approvedTotal = new Counter('panen_approved');
const rejectedTotal = new Counter('panen_rejected');
const emptyQueue    = new Counter('empty_pending_queue');
const reviewCycles  = new Counter('review_cycles');

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

export const options = IS_DRY_RUN
  ? {
      scenarios: {
        mandor_review: {
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
        mandor_review: {
          executor: 'constant-vus',
          vus: 20,
          duration: '30m',
        },
      },
      thresholds: {
        http_req_duration: ['p(95)<500'],
        server_errors: ['rate<0.01'],
      },
      summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
    };

function auth(mandor) {
  return { headers: { 'Authorization': `Bearer ${mandor.token}` } };
}

function jsonAuth(mandor) {
  return {
    headers: {
      'Authorization': `Bearer ${mandor.token}`,
      'Content-Type': 'application/json',
    },
  };
}

export default function () {
  const mandor = mandorUsers[(__VU - 1) % mandorUsers.length];

  // ── Step 1: Fetch pending panen ────────────────────────────────
  const listRes = http.get(
    `${BASE_URL}/api/panen/mandor`,
    { ...auth(mandor), tags: { step: 'fetch_pending_list' } }
  );

  if (listRes.status !== 200) {
    if (listRes.status >= 500) serverErrors.add(1);
    sleep(3);
    return;
  }

  let pendingList = [];
  try {
    const body = JSON.parse(listRes.body);
    pendingList = Array.isArray(body) ? body : (body.data || body.content || []);
    pendingList = pendingList.filter(p => {
      const status = (p.status || '').toUpperCase();
      return status === 'PENDING';
    });
  } catch (_) {
    sleep(2);
    return;
  }

  if (pendingList.length === 0) {
    emptyQueue.add(1);
    sleep(8 + Math.random() * 4); // Longer backoff — nothing to review
    return;
  }
  reviewCycles.add(1);

  // ── Step 2: Choose review action ───────────────────────────────
  // 80% approve, 20% reject. Reject if weight is suspiciously high (>280 kg).
  // This mirrors real mandor behavior: bulk approve normal, flag outliers.
  const target = pendingList[Math.floor(Math.random() * pendingList.length)];
  const panenId = target.id || target.panenId;
  const weight = target.weight || 0;

  const shouldReject = Math.random() < 0.20 || weight > 280;

  const reviewAction = shouldReject ? 'REJECT' : 'APPROVE';
  const reviewPayload = shouldReject
    ? JSON.stringify({ action: 'REJECT', rejectionReason: 'Hasil panen terlalu tinggi, perlu verifikasi ulang' })
    : JSON.stringify({ action: 'APPROVE', rejectionReason: null });

  let reviewRes;
  group('review-panen', () => {
    reviewRes = http.patch(
      `${BASE_URL}/api/panen/${panenId}/review`,
      reviewPayload,
      { ...jsonAuth(mandor), tags: { step: `review_${reviewAction.toLowerCase()}` } }
    );

    const ok = reviewRes.status === 200 || reviewRes.status === 204;
    check(reviewRes, {
      [`review ${reviewAction} ok`]: () => ok,
      'no server error': () => reviewRes.status < 500,
    });

    if (ok && shouldReject) rejectedTotal.add(1);
    if (ok && !shouldReject) approvedTotal.add(1);
    if (reviewRes.status >= 500) serverErrors.add(1);
  });

  // ── Step 3: Mark a notification read (if any) ──────────────────
  // Simulates mandor clearing notifications related to harvest reviews
  if (Math.random() < 0.3) {
    group('mark-notification', () => {
      const notifRes = http.get(
        `${BASE_URL}/api/notifications`,
        { ...auth(mandor), tags: { step: 'fetch_notifications' } }
      );
      if (notifRes.status === 200) {
        try {
          const notifications = JSON.parse(notifRes.body);
          const list = Array.isArray(notifications) ? notifications : (notifications.data || []);
          const unread = list.find(n => !n.isRead);
          if (unread) {
            http.post(
              `${BASE_URL}/api/notifications/${unread.id || unread.notificationId}/read`,
              null,
              { ...auth(mandor), tags: { step: 'mark_read' } }
            );
          }
        } catch (_) { /* skip */ }
      }
    });
  }

  // ── Step 4: Periodically check active deliveries ───────────────
  // Mandor monitors ongoing deliveries (every ~5th review cycle)
  if (Math.random() < 0.2) {
    group('check-active-deliveries', () => {
      http.get(
        `${BASE_URL}/api/pengiriman/mandor/active`,
        { ...auth(mandor), tags: { step: 'active_deliveries' } }
      );
    });
  }

  // Realistic review cadence: 2–5s between reviews
  // (mandor checks details, compares with previous entries, etc.)
  sleep(2 + Math.random() * 3);
}

export function handleSummary(data) {
  return {
    'k6/results/mandor-review-summary.json': JSON.stringify(data, null, 2),
  };
}
