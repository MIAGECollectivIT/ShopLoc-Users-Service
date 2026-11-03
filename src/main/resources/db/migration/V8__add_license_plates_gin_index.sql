-- Migration V8: Index client license plates for municipal parking verification
-- A GIN (Generalized Inverted Index) on PostgreSQL enables efficient containment and overlap lookups:
-- e.g. 'AA-123-AA' = ANY(license_plates) or license_plates @> ARRAY['AA-123-AA']

CREATE INDEX IF NOT EXISTS idx_clients_license_plates ON clients USING GIN (license_plates);
