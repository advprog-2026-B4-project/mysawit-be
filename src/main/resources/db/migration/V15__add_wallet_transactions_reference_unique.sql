-- NOTE:
-- Module-level schema separation was removed.
-- All tables live in the default schema (public) for simplicity.

-- Ensure we have a stable uniqueness guarantee for external payment references.
DROP INDEX IF EXISTS idx_wallet_transactions_reference;
ALTER TABLE wallet_transactions
	ADD CONSTRAINT uq_wallet_transactions_reference UNIQUE (reference);
