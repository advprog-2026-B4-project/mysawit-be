ALTER TABLE pengiriman
    ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);

CREATE TABLE IF NOT EXISTS pengiriman_panen_item (
    pengiriman_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pengiriman_id UUID NOT NULL REFERENCES pengiriman (pengiriman_id) ON DELETE CASCADE,
    panen_id UUID NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_pengiriman_item_pengiriman
    ON pengiriman_panen_item (pengiriman_id);

CREATE INDEX IF NOT EXISTS idx_pengiriman_item_panen
    ON pengiriman_panen_item (panen_id);
