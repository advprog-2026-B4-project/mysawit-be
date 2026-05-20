-- Prevent duplicate Midtrans top-up credits under concurrent replicas.
-- existsByReference() check in WalletRepositoryAdapter is not atomic;
-- this constraint is the authoritative guard.
DROP INDEX IF EXISTS idx_wallet_transactions_reference;

ALTER TABLE pembayaran.wallet_transactions
    ADD CONSTRAINT uq_wallet_transactions_reference UNIQUE (reference);
