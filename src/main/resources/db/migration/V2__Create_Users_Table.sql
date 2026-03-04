-- V2: Auth module - users table

CREATE TABLE users (
    user_id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    password      VARCHAR(255),               -- NULL for OAuth-only accounts
    role          VARCHAR(20)  NOT NULL,
    mandor_id     UUID,                       -- non-null only when role = BURUH
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email  ON users(email);
CREATE INDEX idx_users_role   ON users(role);
CREATE INDEX idx_users_mandor ON users(mandor_id);

-- Default Admin account  (password plain-text: Admin@12345)
INSERT INTO users (user_id, username, email, name, password, role)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@mysawit.id',
    'Admin Utama',
    'AdminKelompok4',
    'ADMIN'
);