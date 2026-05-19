-- Widen money columns from INTEGER to BIGINT to support large IDR amounts
ALTER TABLE wallets ALTER COLUMN balance TYPE BIGINT;
ALTER TABLE wallet_transactions ALTER COLUMN amount TYPE BIGINT;
