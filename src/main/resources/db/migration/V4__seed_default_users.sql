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
-- Admin (no license plate)
(
    gen_random_uuid(),
    'marius.vasseur@shop-loc.fr',
    '$2a$12$D51DIpyuiyVRAwwksViBn.p00Kwja/P8C8pQEX0otOfLW04jP1.km',
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
    '$2a$12$SHi/ItBrg3WAk06fcfSn2u/UTDdiz7orRkXNILCY8SYLVcRLX2626',
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
    '$2a$12$zFGvSqdghAUf7JY68HIUjO1rmLIUvm/5LGtQLNFkIDtPH3RScubGu',
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