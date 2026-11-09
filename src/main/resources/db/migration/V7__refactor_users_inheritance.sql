-- Migration V7: Refactor users into joined table inheritance (users, clients, merchants, admins)

-- 1. Create clients table
CREATE TABLE IF NOT EXISTS clients (
    id BIGINT PRIMARY KEY,
    client_code VARCHAR(7) NOT NULL UNIQUE,
    pickup_location VARCHAR(150),
    license_plates TEXT[] NOT NULL DEFAULT '{}',
    CONSTRAINT fk_clients_user FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_clients_client_code ON clients (client_code);

-- 2. Create merchants table
CREATE TABLE IF NOT EXISTS merchants (
    id BIGINT PRIMARY KEY,
    CONSTRAINT fk_merchants_user FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
);

-- 3. Create admins table
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT PRIMARY KEY,
    CONSTRAINT fk_admins_user FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
);

-- 4. Transfer existing data from users to specialized tables
INSERT INTO clients (id, client_code, pickup_location, license_plates)
SELECT id, client_code, pickup_location, license_plates
FROM users
WHERE role = 'CLIENT'
ON CONFLICT (id) DO NOTHING;

INSERT INTO merchants (id)
SELECT id
FROM users
WHERE role = 'MERCHANT'
ON CONFLICT (id) DO NOTHING;

INSERT INTO admins (id)
SELECT id
FROM users
WHERE role = 'ADMIN'
ON CONFLICT (id) DO NOTHING;

-- 5. Drop client-specific columns from users table
DROP INDEX IF EXISTS idx_users_client_code;
ALTER TABLE users DROP COLUMN IF EXISTS client_code;
ALTER TABLE users DROP COLUMN IF EXISTS pickup_location;
ALTER TABLE users DROP COLUMN IF EXISTS license_plates;

-- 6. Create convenience view for clients
CREATE OR REPLACE VIEW view_clients AS
SELECT 
    u.id,
    u.public_id,
    u.email,
    u.password_hash,
    u.role,
    u.first_name,
    u.last_name,
    u.birth_date,
    c.client_code,
    c.pickup_location,
    c.license_plates,
    u.failed_login_attempts,
    u.locked_until,
    u.created_at,
    u.updated_at
FROM users u
INNER JOIN clients c ON u.id = c.id;
