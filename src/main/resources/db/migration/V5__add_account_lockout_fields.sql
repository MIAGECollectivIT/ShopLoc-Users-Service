-- Migration V5: Add account lockout fields for CNIL brute-force protection compliance
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE NULL;

CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users (locked_until);
