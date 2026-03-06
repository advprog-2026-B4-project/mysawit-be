-- Module 4: Manajemen Pengiriman - delivery records
CREATE TABLE IF NOT EXISTS pengiriman (
    pengiriman_id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    supir_id        UUID         NOT NULL,
    mandor_id       UUID         NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    total_weight    INTEGER      NOT NULL CHECK (total_weight >= 0),
    accepted_weight INTEGER      NOT NULL DEFAULT 0 CHECK (accepted_weight >= 0),
    timestamp       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pengiriman_supir_timestamp
    ON pengiriman (supir_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_pengiriman_mandor_timestamp
    ON pengiriman (mandor_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_pengiriman_status
    ON pengiriman (status);
