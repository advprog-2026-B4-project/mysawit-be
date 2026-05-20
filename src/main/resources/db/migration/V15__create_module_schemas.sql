-- Create per-module schemas for logical isolation (T2-2)
-- Postgres transactions cross schemas freely; @Transactional still works.
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS kebun;
CREATE SCHEMA IF NOT EXISTS panen;
CREATE SCHEMA IF NOT EXISTS pembayaran;
CREATE SCHEMA IF NOT EXISTS pengiriman;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS common;

-- Move existing tables into module schemas
ALTER TABLE users              SET SCHEMA auth;
ALTER TABLE kebun              SET SCHEMA kebun;
ALTER TABLE kebun_supir        SET SCHEMA kebun;
ALTER TABLE harvest_reports    SET SCHEMA panen;
ALTER TABLE harvest_photos     SET SCHEMA panen;
ALTER TABLE payrolls           SET SCHEMA pembayaran;
ALTER TABLE variables          SET SCHEMA pembayaran;
ALTER TABLE wallets            SET SCHEMA pembayaran;
ALTER TABLE wallet_transactions SET SCHEMA pembayaran;
ALTER TABLE pengiriman         SET SCHEMA pengiriman;
ALTER TABLE pengiriman_panen_item SET SCHEMA pengiriman;
ALTER TABLE notifications      SET SCHEMA notification;
ALTER TABLE health_check       SET SCHEMA common;
