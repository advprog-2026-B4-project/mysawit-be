DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT conname, conrelid::regclass AS tbl
        FROM pg_constraint
        WHERE contype = 'f'
          AND conrelid IN (
              'harvest_reports'::regclass,
              'payrolls'::regclass,
              'wallets'::regclass,
              'wallet_transactions'::regclass
          )
          AND confrelid IN (
              'users'::regclass,
              'kebun'::regclass
          )
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I', r.tbl, r.conname);
        RAISE NOTICE 'Dropped FK: %.%  →  (cross-module)', r.tbl, r.conname;
    END LOOP;
END $$;
