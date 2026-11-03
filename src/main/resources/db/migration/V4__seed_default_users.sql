-- Migration V4: Initial provisioned seed users with human identities

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
-- Admin
(
    gen_random_uuid(),
    'marius.vasseur@shop-loc.fr',
    '$2a$12$2ebARMeajpwYW14E4IDUhOjGVTl3u9bFtZy76.iqK.UZoOZp1YWwm',
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
-- Merchant
(
    gen_random_uuid(),
    'suzanne.morel@shop-loc.fr',
    '$2a$12$WIp8VuYIjAUgtbFinTv8y.0PFkl2sFUapw5ta29D.A4BibO2CFOmW',
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
-- Client
(
    gen_random_uuid(),
    'pierre.dufour@shop-loc.fr',
    '$2a$12$Ix1Bh3bVR3rUTj.K8qQS4uhrxMQi0mXTgBNESY3mHzECbelDZBh2a',
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