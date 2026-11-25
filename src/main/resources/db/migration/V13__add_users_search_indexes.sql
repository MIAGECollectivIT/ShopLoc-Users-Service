-- Migration V13: Indexes to optimize admin user search and eligibility filtering (SLE-274)
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users (locked_until);
CREATE INDEX IF NOT EXISTS idx_users_created_at_desc ON users (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_first_name_lower ON users (LOWER(first_name));
CREATE INDEX IF NOT EXISTS idx_users_last_name_lower ON users (LOWER(last_name));
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
