-- Migration V10: Add store_uid (UUID) to user_stores for strict UUID exposure
ALTER TABLE user_stores ADD COLUMN IF NOT EXISTS store_uid UUID;
ALTER TABLE user_stores ALTER COLUMN store_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_store_uid ON user_stores (user_id, store_uid);
CREATE INDEX IF NOT EXISTS idx_user_stores_store_uid ON user_stores (store_uid);
