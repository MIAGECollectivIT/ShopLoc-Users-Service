-- Migration V6: Ensure empty or whitespace pickup locations are stored as canonical NULL
UPDATE users
SET pickup_location = NULL
WHERE pickup_location IS NOT NULL AND TRIM(pickup_location) = '';
