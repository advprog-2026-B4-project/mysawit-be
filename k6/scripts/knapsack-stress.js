/**
 * Scenario 6 — Knapsack Recommendation Stress Test
 *
 * Targets the CPU-bound dynamic programming knapsack algorithm at
 * PengirimanQueryUseCaseImpl.solveKnapsack() — a subset-sum DP under
 * a 400 kg capacity constraint that bundles harvest items for delivery.
 *
 * This is the single most computationally expensive read endpoint in the
 * system. It runs in O(n * capacity) where n is the number of assignable
 * panen items. At scale, this can become a CPU bottleneck.
 *
 * Test design:
 *   - 10 MANDOR VU hammering the recommendation endpoint for 5 minutes
 *   - 50% with default capacity (400 kg), 50% with random capacities
 *   - After each recommendation, one VU may also create an assignment
 *     (POST /api/pengiriman) — exercising the full read→write pipeline
 *
 * WHAT TO MONITOR:
 *   - CPU usage: system_cpu_usage, process_cpu_usage
 *   - JVM heap: jvm_memory_used_bytes{area="heap"}
 *   - GC activity: jvm_gc_pause_seconds (watch for frequent full GC)
 *
 * SLA:
 *   - p95 < 2000ms (knapsack is CPU-bound, give it room)
 *   - 5xx error rate < 1%
 *   - CPU must not saturate at steady state
 *
 * Run: k6 run --out json=k6/results/knapsack-stress.json k6/scripts/knapsack-stress.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const IS_DRY_RUN = (__ENV.DRY_RUN || '').toLowerCase() === '1' || (__ENV.DRY_RUN || '').toLowerCase() === 'true';

const serverErrors     = new Rate('server_errors');
const knapsackCalls    = new Counter('knapsack_calls');
const knapsackDuration = new Trend('knapsack_duration_ms', true); // true = collect as time
const assignmentsMade  = new Counter('assignments_created');
const emptyPanen       = new Counter('empty_assignable_panen');

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
        knapsack_test: {
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
        knapsack_test: {
          executor: 'constant-vus',
          vus: 10,
          duration: '5m',
        },
      },
      thresholds: {
        http_req_duration: ['p(95)<2000'],
        knapsack_duration_ms: ['p(95)<1500'],
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

  // Vary capacities to exercise different knapsack problem sizes:
  // default 400 kg (full load), 200 kg (half), 100 kg (small)
  const capacities = [400, 200, 100, null]; // null = server default
  const capacity = capacities[Math.floor(Math.random() * capacities.length)];
  const capQuery = capacity ? `?maxCapacity=${capacity}` : '';

  // ── Step 1: Get recommendation ────────────────────────────────
  let recommendRes;
  let recommendOk = false;
  let panenIds = [];

  group('recommendation', () => {
    const callStart = Date.now();
    recommendRes = http.get(
      `${BASE_URL}/api/pengiriman/mandor/recommendation${capQuery}`,
      { ...auth(mandor), tags: { step: 'knapsack_recommend' } }
    );
    knapsackDuration.add(Date.now() - callStart);
    knapsackCalls.add(1);

    recommendOk = recommendRes.status === 200;
    check(recommendRes, {
      'recommend 200': () => recommendOk,
      'no server error': () => recommendRes.status < 500,
    });

    if (recommendRes.status >= 500) serverErrors.add(1);

    // Extract recommended panen IDs for optional assignment step
    if (recommendOk) {
      try {
        const body = JSON.parse(recommendRes.body);
        const data = body.data || body;
        if (data && data.recommendedCombination) {
          panenIds = (data.recommendedCombination || [])
            .map(item => item.panenId || item.id)
            .filter(Boolean);
        }
      } catch (_) { /* parse failure — skip assignment */ }
    }
  });

  if (panenIds.length === 0) {
    emptyPanen.add(1);
    sleep(3 + Math.random() * 5);
    return;
  }

  // ── Step 2: 20% chance — act on the recommendation ────────────
  // Simulates mandor actually creating a pengiriman assignment
  if (Math.random() < 0.20) {
    group('create-assignment', () => {
      // Pick a random supir assigned to this mandor's kebun
      const supirRes = http.get(
        `${BASE_URL}/api/pengiriman/mandor/supir`,
        { ...auth(mandor), tags: { step: 'fetch_supir' } }
      );

      let supirId = null;
      if (supirRes.status === 200) {
        try {
          const body = JSON.parse(supirRes.body);
          const supirList = Array.isArray(body) ? body : (body.data || []);
          if (supirList.length > 0) {
            const supir = supirList[Math.floor(Math.random() * supirList.length)];
            supirId = supir.supirId || supir.userId || supir.id;
          }
        } catch (_) { /* skip */ }
      }

      if (supirId) {
        const assignPayload = JSON.stringify({
          supirId: supirId,
          panenIds: panenIds.slice(0, Math.min(panenIds.length, 3)),
        });

        const assignRes = http.post(
          `${BASE_URL}/api/pengiriman`,
          assignPayload,
          { ...jsonAuth(mandor), tags: { step: 'assign_delivery' } }
        );

        const is201 = assignRes.status === 201;
        check(assignRes, {
          'assignment created': () => is201 || assignRes.status === 200,
          'no server error': () => assignRes.status < 500,
        });

        if (is201 || assignRes.status === 200) assignmentsMade.add(1);
        if (assignRes.status >= 500) serverErrors.add(1);
      }
    });
  }

  // Think time: 2–6s (mandor evaluates recommendation before acting)
  sleep(2 + Math.random() * 4);
}

export function handleSummary(data) {
  return {
    'k6/results/knapsack-stress-summary.json': JSON.stringify(data, null, 2),
  };
}
