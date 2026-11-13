-- What: plaza dataset, version metadata, and spatial indexes (PostgreSQL 16+, PostGIS 3.2+).
-- Why: meter-based route matching and cache invalidation after dataset changes.
-- Apply: after explicit approval, run from the repository root against the intended database:
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f docs/database/0001-toll-plaza-schema.sql
-- Verify: SELECT postgis_version(); SELECT * FROM toll_dataset_metadata;
--         SELECT indexname FROM pg_indexes WHERE tablename = 'toll_plaza';
BEGIN;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS toll_dataset_metadata (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);
INSERT INTO toll_dataset_metadata(singleton, version) VALUES (true, 0) ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS toll_plaza (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL CHECK (length(btrim(name)) > 0),
    state text NOT NULL CHECK (length(btrim(state)) > 0),
    location geometry(Point, 4326) NOT NULL,
    CONSTRAINT valid_coordinates CHECK (
        NOT ST_IsEmpty(location) AND ST_X(location) BETWEEN -180 AND 180 AND ST_Y(location) BETWEEN -90 AND 90
    )
);
CREATE UNIQUE INDEX IF NOT EXISTS toll_plaza_identity ON toll_plaza (name, state, ST_X(location), ST_Y(location));
CREATE INDEX IF NOT EXISTS toll_plaza_location_idx ON toll_plaza USING gist (location);
CREATE INDEX IF NOT EXISTS toll_plaza_geography_idx ON toll_plaza USING gist ((location::geography));

CREATE OR REPLACE FUNCTION bump_toll_dataset_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE toll_dataset_metadata SET version = version + 1 WHERE singleton = true;
    RETURN NULL;
END;
$$;
DROP TRIGGER IF EXISTS toll_dataset_changed ON toll_plaza;
CREATE TRIGGER toll_dataset_changed AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON toll_plaza
    FOR EACH STATEMENT EXECUTE FUNCTION bump_toll_dataset_version();
COMMIT;
