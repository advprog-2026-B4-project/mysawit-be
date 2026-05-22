-- =============================================================================
-- MySawit Load Test — Database Seeding Script
-- Run via: psql -U postgres -d mysawit -f seed-loadtest.sql
--
-- Volume:
--   50 mandor · 500 buruh · 100 supir · 50 kebun · 100 kebun_supir
--   90 000 APPROVED harvest_reports  (500 buruh × 180 days, days 1–180)
--   18 000 PENDING  harvest_reports  (100 buruh × 180 days, days 181–360)
--   108 000 payrolls · 5 000 pengiriman · ~32 500 notifications · 651 wallets
-- =============================================================================

-- Assumes Flyway migrations have already run:
--   V2 → admin user
--   V5 → variables (UPAH_BURUH / UPAH_SUPIR / UPAH_MANDOR)
--
-- TEST_PW  = bcrypt of "Admin@12345" (same hash used by Flyway admin seed)
-- LT_PAT   = regex identifying every load-test username
\set TEST_PW  '$2a$12$LcmKR1vpQaWK3GsKFBGbsex9f1IVbSnP5xOtoF7BT4E0.V7BqkXhS'
\set LT_PAT   '^(buruh|supir|mandor)_[0-9]+$'

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Users  (mandor first — buruh FKs into mandor)
-- ---------------------------------------------------------------------------
INSERT INTO users (user_id, username, email, name, password, role)
SELECT gen_random_uuid(),
       'mandor_' || i,
       'mandor_' || i || '@loadtest.mysawit.id',
       'Mandor Kebun '   || i,
       :'TEST_PW', 'MANDOR'
FROM generate_series(1, 50) AS i
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (user_id, username, email, name, password, role, mandor_id)
SELECT gen_random_uuid(),
       'buruh_' || i,
       'buruh_'  || i || '@loadtest.mysawit.id',
       'Buruh '  || i,
       :'TEST_PW', 'BURUH',
       m.user_id
FROM generate_series(1, 500) AS i
JOIN (
  SELECT user_id, ROW_NUMBER() OVER (ORDER BY username) AS rn
  FROM   users WHERE role = 'MANDOR' AND username ~ :'LT_PAT'
) m ON m.rn = ((i - 1) / 10 + 1)   -- 10 buruh per mandor
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (user_id, username, email, name, password, role)
SELECT gen_random_uuid(),
       'supir_' || i,
       'supir_'  || i || '@loadtest.mysawit.id',
       'Supir '  || i,
       :'TEST_PW', 'SUPIR'
FROM generate_series(1, 100) AS i
ON CONFLICT (username) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Kebun  (one per mandor)
-- ---------------------------------------------------------------------------
INSERT INTO kebun (kebun_id, nama, kode, luas, mandor_id)
SELECT gen_random_uuid(),
       'Kebun Load Test ' || rn,
       'KBN-LT-' || LPAD(rn::TEXT, 3, '0'),
       (100 + (RANDOM() * 300)::INT),
       user_id
FROM (
  SELECT user_id, ROW_NUMBER() OVER (ORDER BY username) AS rn
  FROM   users WHERE role = 'MANDOR' AND username ~ :'LT_PAT'
) m
ON CONFLICT (kode) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Kebun–Supir mapping  (2 supir per kebun via modular assignment)
--    supir rn 1–50  → kebun rn 1–50
--    supir rn 51–100 → kebun rn 1–50  (wraps via % 50)
-- ---------------------------------------------------------------------------
INSERT INTO kebun_supir (id, kebun_id, supir_id)
SELECT gen_random_uuid(), k.kebun_id, s.user_id
FROM (
  SELECT kebun_id, ROW_NUMBER() OVER (ORDER BY kode) AS rn
  FROM   kebun WHERE kode LIKE 'KBN-LT-%'
) k
JOIN (
  SELECT user_id, ((ROW_NUMBER() OVER (ORDER BY username) - 1) % 50) + 1 AS kebun_rn
  FROM   users WHERE role = 'SUPIR' AND username ~ :'LT_PAT'
) s ON s.kebun_rn = k.rn
ON CONFLICT ON CONSTRAINT uk_supir_one_kebun DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Wallets
-- ---------------------------------------------------------------------------
INSERT INTO wallets (user_id, balance, updated_at)
SELECT user_id, 0, NOW()
FROM   users WHERE username ~ :'LT_PAT'
ON CONFLICT (user_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. Harvest reports  (APPROVED days 1–180 · PENDING days 181–360)
--    Both batches share the same shape; status and range differ only.
--    PENDING is limited to the first 100 buruh (18 000 records for Skenario 3).
-- ---------------------------------------------------------------------------
WITH buruh AS (
  SELECT user_id, mandor_id,
         ROW_NUMBER() OVER (ORDER BY username) AS rn
  FROM   users WHERE role = 'BURUH' AND username ~ :'LT_PAT'
)
INSERT INTO harvest_reports
       (id, buruh_id, kebun_id, weight, status, harvest_date, created_at)
SELECT gen_random_uuid(),
       b.user_id,
       k.kebun_id,
       (50 + (RANDOM() * 250)::INT),
       CASE WHEN d.days_ago <= 180 THEN 'APPROVED' ELSE 'PENDING' END,
       (CURRENT_DATE  - d.days_ago)::DATE,
       (NOW() - (d.days_ago || ' days')::INTERVAL
              + (RANDOM() * 28800  || ' seconds')::INTERVAL)
FROM   buruh b
JOIN   kebun k ON k.mandor_id = b.mandor_id
CROSS JOIN generate_series(1, 360) AS d(days_ago)
WHERE  d.days_ago <= 180          -- APPROVED: all 500 buruh
    OR b.rn       <= 100          -- PENDING:  first 100 buruh only
ON CONFLICT ON CONSTRAINT daily_harvest DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Payrolls  (one per APPROVED harvest)
-- ---------------------------------------------------------------------------
INSERT INTO payrolls (
  payroll_id, user_id, role, reference_id, reference_type,
  weight, wage_rate_applied, net_amount, status, processed_at, created_at
)
SELECT gen_random_uuid(),
       hr.buruh_id, 'BURUH', hr.id, 'PANEN',
       hr.weight, 10000, hr.weight * 10000,
       'APPROVED',
       hr.created_at + INTERVAL '1 hour',
       hr.created_at
FROM   harvest_reports hr
JOIN   users u ON u.user_id = hr.buruh_id AND u.username ~ :'LT_PAT'
WHERE  hr.status = 'APPROVED'
ON CONFLICT ON CONSTRAINT uk_payrolls_user_role_reference DO NOTHING;

-- ---------------------------------------------------------------------------
-- 7. Pengiriman  (5 000 historical, terminal status)
-- ---------------------------------------------------------------------------
INSERT INTO pengiriman
       (pengiriman_id, supir_id, mandor_id, status,
        total_weight, accepted_weight, timestamp)
SELECT gen_random_uuid(),
       s.user_id, k.mandor_id, 'APPROVED_ADMIN',
       (500  + (RANDOM() * 3000)::INT),
       (400  + (RANDOM() * 2800)::INT),
       NOW() - ((RANDOM() * 180)::INT || ' days')::INTERVAL
FROM (
  SELECT u.user_id, ks.kebun_id
  FROM   users u
  JOIN   kebun_supir ks ON ks.supir_id = u.user_id
  WHERE  u.role = 'SUPIR' AND u.username ~ :'LT_PAT'
) s
JOIN   kebun k ON k.kebun_id = s.kebun_id
CROSS JOIN generate_series(1, 50);

-- ---------------------------------------------------------------------------
-- 8. Notifications  (50 per load-test user ≈ 32 500 total)
-- ---------------------------------------------------------------------------
INSERT INTO notifications
       (notification_id, user_id, title, description, is_read, timestamp)
SELECT gen_random_uuid(),
       u.user_id,
       (ARRAY[
         'Laporan Panen Disetujui',
         'Pembayaran Diterima',
         'Pengiriman Selesai',
         'Notifikasi Sistem'
       ])[ (RANDOM() * 3)::INT + 1 ],
       'Notifikasi load test #' || i || ' untuk ' || u.name,
       (RANDOM() > 0.3),
       NOW() - ((RANDOM() * 180)::INT || ' days')::INTERVAL
FROM   users u
CROSS JOIN generate_series(1, 50) AS i
WHERE  u.username ~ :'LT_PAT';

-- ---------------------------------------------------------------------------
-- 9. Update query-planner statistics  (critical after bulk insert)
-- ---------------------------------------------------------------------------
ANALYZE;

COMMIT;

-- Verification
SELECT tabel, total FROM (VALUES
  ('users (lt)',          (SELECT COUNT(*) FROM users           WHERE username ~ :'LT_PAT')),
  ('kebun (lt)',          (SELECT COUNT(*) FROM kebun           WHERE kode LIKE 'KBN-LT-%')),
  ('wallets',             (SELECT COUNT(*) FROM wallets)),
  ('harvest APPROVED',    (SELECT COUNT(*) FROM harvest_reports WHERE status = 'APPROVED')),
  ('harvest PENDING',     (SELECT COUNT(*) FROM harvest_reports WHERE status = 'PENDING')),
  ('payrolls',            (SELECT COUNT(*) FROM payrolls)),
  ('pengiriman',          (SELECT COUNT(*) FROM pengiriman)),
  ('notifications',       (SELECT COUNT(*) FROM notifications))
) AS t(tabel, total);
