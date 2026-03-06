CREATE TABLE health_check (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Masukkan satu baris data dummy untuk dites nanti
INSERT INTO health_check (service_name, status) VALUES ('MySawit-Backend', 'OK');