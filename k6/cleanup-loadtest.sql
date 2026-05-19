-- =============================================================================
-- MySawit Load Test — Cleanup / Teardown Script
-- Run via: psql -U postgres -d mysawit -f cleanup-loadtest.sql
--
-- Removes all data inserted by seed-loadtest.sql.
-- Preserves: admin user, Flyway migration history, variables.
-- =============================================================================

\set LT_PAT '^(buruh|supir|mandor)_[0-9]+$'

BEGIN;

TRUNCATE TABLE
    notifications, wallet_transactions, payrolls,
    pengiriman_panen_item, pengiriman,
    harvest_photos, harvest_reports,
    wallets, kebun_supir, kebun_coordinate, kebun
CASCADE;

DELETE FROM users WHERE username ~ :'LT_PAT';

ANALYZE;

COMMIT;

-- Verification (mirrors seed verification)
SELECT tabel, total FROM (VALUES
  ('users remaining',    (SELECT COUNT(*) FROM users)),
  ('kebun remaining',    (SELECT COUNT(*) FROM kebun)),
  ('harvest remaining',  (SELECT COUNT(*) FROM harvest_reports)),
  ('payrolls remaining', (SELECT COUNT(*) FROM payrolls)),
  ('notif remaining',    (SELECT COUNT(*) FROM notifications))
) AS t(tabel, total);
