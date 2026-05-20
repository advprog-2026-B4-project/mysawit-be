ALTER TABLE users
    ADD COLUMN mandor_certification_number VARCHAR(100);

CREATE INDEX idx_users_mandor_certification_number
    ON users(mandor_certification_number);
