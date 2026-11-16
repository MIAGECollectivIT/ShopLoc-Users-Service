-- Migration V9: Create physical card notifications table for logging card order dispatches (SLE-35, SLE-217)
CREATE TABLE IF NOT EXISTS physical_card_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    client_code VARCHAR(7) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    pickup_location VARCHAR(150) NOT NULL,
    service_recipient_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_physical_card_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pcn_user_id ON physical_card_notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_pcn_status ON physical_card_notifications (status);
CREATE INDEX IF NOT EXISTS idx_pcn_client_code ON physical_card_notifications (client_code);

