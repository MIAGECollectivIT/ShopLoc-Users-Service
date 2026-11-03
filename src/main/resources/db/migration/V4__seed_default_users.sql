-- Migration V4: Initial provisioned seed users with human identities
-- Passwords:
-- Admin:    a.vasseur82@shop-loc.fr / K9#m$Qv7!dF2@wX8&yLp
-- Merchant: c.morel47@shop-loc.fr   / B4&x!T9@vR6#mZ1$wN8*
-- Client:   j.dufour95@shop-loc.fr  / H7*q#V2!yL9@sD4&zP3%

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
-- Administrateur (Alexandre Vasseur)
(
    gen_random_uuid(),
    'a.vasseur82@shop-loc.fr',
    '$2b$12$OSo6j4kbXSBnEpO0W4S8j.xD7AIW2g1VJlHZUOlC6GwUH03Ztg30q',
    'ADMIN',
    'Alexandre',
    'Vasseur',
    '1982-04-12',
    'AD84912',
    'Mairie Centrale',
    '{}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
-- Commerçant Partenaire (Camille Morel)
(
    gen_random_uuid(),
    'c.morel47@shop-loc.fr',
    '$2b$12$yVg1vy8GRG4o.LacPpLw..g/PffoLoHFlkPxqFvTbzsi6WWPTKAGO',
    'MERCHANT',
    'Camille',
    'Morel',
    '1987-09-23',
    'ME61028',
    'Comptoir des Halles',
    '{}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
-- Client Adhérent (Julien Dufour)
(
    gen_random_uuid(),
    'j.dufour95@shop-loc.fr',
    '$2b$12$3hmn0i6r8BJA0TZ0T55IwezbW23IKNsB6y1Rh09L9yxzIlhGWIHAa',
    'CLIENT',
    'Julien',
    'Dufour',
    '1995-11-04',
    'CL73954',
    'Mairie Annexe Ouest',
    '{"AA-123-BB"}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
