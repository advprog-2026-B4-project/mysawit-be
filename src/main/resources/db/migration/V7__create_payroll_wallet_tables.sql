-- Module 5: Payroll & wallet tables
CREATE TABLE IF NOT EXISTS payrolls (
    payroll_id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL REFERENCES users(user_id),
    role               VARCHAR(20)  NOT NULL,
    reference_id       UUID         NOT NULL,
    reference_type     VARCHAR(20)  NOT NULL,
    weight             INTEGER      NOT NULL CHECK (weight >= 0),
    wage_rate_applied  INTEGER      NOT NULL CHECK (wage_rate_applied >= 0),
    net_amount         INTEGER      NOT NULL CHECK (net_amount >= 0),
    status             VARCHAR(20)  NOT NULL,
    rejection_reason   TEXT,
    processed_at       TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    payment_reference  VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_payrolls_user_created_at
    ON payrolls (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payrolls_status_created_at
    ON payrolls (status, created_at DESC);

CREATE TABLE IF NOT EXISTS wallets (
    user_id     UUID      PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    balance     INTEGER   NOT NULL DEFAULT 0 CHECK (balance >= 0),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    transaction_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    payroll_id     UUID        REFERENCES payrolls(payroll_id) ON DELETE SET NULL,
    amount         INTEGER     NOT NULL CHECK (amount >= 0),
    type           VARCHAR(32) NOT NULL,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_created_at
    ON wallet_transactions (user_id, created_at DESC);
