-- Migration V4: Initial provisioned seed users with human identities
-- Passwords:
-- Admin:    marius.vasseur@shop-loc.fr / K9#m$Qv7!dF2@wX8&yLp
-- Merchant: suzanne.morel@shop-loc.fr  / B4&x!T9@vR6#mZ1$wN8*
-- Client:   pierre.dufour@shop-loc.fr  / H7*q#V2!yL9@sD4&zP3%

INSERT INTO users (
    public_id,
    email,
    password_hash,
    role,
    first_name,
    last_name,
    birth_date,
    client_code,
    pickup_location,
    license_plates,
    created_at,
    updated_at
) VALUES
-- Admin (no license plate)
(
    gen_random_uuid(),
    'marius.vasseur@shop-loc.fr',
    '$2a$12$/0saCJR6LtblBFuxIwV.sOOs.yuZN8raMXB0QrrwUETbAo.AyHqga',
    'ADMIN',
    'Marius',
    'Vasseur',
    '1982-05-14',
    'AD82001',
    NULL,
    '{}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
-- Merchant (no license plate)
(
    gen_random_uuid(),
    'suzanne.morel@shop-loc.fr',
    '$2a$12$9Q8vhsl1SQw86s5sK87SaOHuF4MeHpxVTFaBGRnWspcqyAbpcnkAG',
    'MERCHANT',
    'Suzanne',
    'Morel',
    '1987-09-23',
    'MR47001',
    NULL,
    '{}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
-- Client (only the client has a registered license plate)
(
    gen_random_uuid(),
    'pierre.dufour@shop-loc.fr',
    '$2a$12$mLkqaOQW2e64OXyu129YuuA55fbNQycdGa0qm1Z5BteoOfUtnu5H6',
    'CLIENT',
    'Pierre',
    'Dufour',
    '1995-11-04',
    'CL95001',
    NULL,
    '{"AA-123-BB"}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;