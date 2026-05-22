-- Add reference column for Midtrans top-up transactions
ALTER TABLE wallet_transactions
ADD COLUMN IF NOT EXISTS reference VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_reference
ON wallet_transactions (reference)
WHERE reference IS NOT NULL;