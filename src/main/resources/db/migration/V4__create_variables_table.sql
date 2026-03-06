-- Module 5: Manajemen Pembayaran - core wage variables
CREATE TABLE IF NOT EXISTS variables (
    key   VARCHAR(50)  NOT NULL,
    value INTEGER      NOT NULL,
    CONSTRAINT pk_variables PRIMARY KEY (key),
    CONSTRAINT chk_variables_value_positive CHECK (value > 0)
);

-- Seed default wage rates (SawitDollar per kg).
INSERT INTO variables (key, value) VALUES
    ('UPAH_BURUH',  10000),
    ('UPAH_SUPIR',   8000),
    ('UPAH_MANDOR', 12000)
ON CONFLICT (key) DO NOTHING;
