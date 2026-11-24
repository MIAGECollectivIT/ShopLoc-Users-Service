-- Migration V10: Create system_settings table for platform configurations (SLE-35)
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255)
);

INSERT INTO system_settings (setting_key, setting_value, updated_at, updated_by)
VALUES ('citizen_card_service_email', 'citoyen-numerique@shop-loc.fr', CURRENT_TIMESTAMP, 'SYSTEM')
ON CONFLICT (setting_key) DO NOTHING;

