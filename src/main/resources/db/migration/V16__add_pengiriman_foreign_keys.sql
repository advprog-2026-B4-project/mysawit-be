-- V16: Add foreign keys to pengiriman for cross-module referential integrity.
-- Ensures supir_id and mandor_id reference valid users, consistent with
-- how harvest_reports.buruh_id/kebun_id (V6) and payrolls/wallets.user_id (V7)
-- already enforce FK constraints across modules.

-- Rollback: ALTER TABLE pengiriman DROP CONSTRAINT IF EXISTS fk_pengiriman_supir;
-- Rollback: ALTER TABLE pengiriman DROP CONSTRAINT IF EXISTS fk_pengiriman_mandor;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_pengiriman_supir'
          AND conrelid = 'pengiriman'::regclass
    ) THEN
        ALTER TABLE pengiriman
            ADD CONSTRAINT fk_pengiriman_supir
            FOREIGN KEY (supir_id) REFERENCES users(user_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_pengiriman_mandor'
          AND conrelid = 'pengiriman'::regclass
    ) THEN
        ALTER TABLE pengiriman
            ADD CONSTRAINT fk_pengiriman_mandor
            FOREIGN KEY (mandor_id) REFERENCES users(user_id);
    END IF;
END $$;
