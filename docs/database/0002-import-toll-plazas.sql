-- What: validate CSV and replace the plaza dataset atomically, removing exact duplicates.
-- Why: make the supplied CSV the authoritative snapshot; rerunning does not add duplicates.
-- Apply: after approval, run from the repository root with psql (not a generic SQL runner):
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f docs/database/0002-import-toll-plazas.sql
-- This REPLACES all rows in toll_plaza. Existing hand-added rows will be removed.
-- Verify: SELECT count(*) FROM toll_plaza; SELECT * FROM toll_dataset_metadata;
--         The import prints input, distinct, and duplicate counts before replacement.
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE toll_csv_input (longitude text, latitude text, toll_name text, geo_state text) ON COMMIT DROP;
\copy toll_csv_input FROM 'Dataset/toll_plaza_india.csv' WITH (FORMAT csv, HEADER MATCH, ENCODING 'UTF8')

DO $$
DECLARE invalid_count bigint;
BEGIN
    SELECT count(*) INTO invalid_count FROM toll_csv_input
    WHERE NOT COALESCE(pg_input_is_valid(longitude, 'double precision'), false)
       OR NOT COALESCE(pg_input_is_valid(latitude, 'double precision'), false)
       OR length(btrim(COALESCE(toll_name, ''))) = 0
       OR length(btrim(COALESCE(geo_state, ''))) = 0;
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'CSV has % rows with invalid coordinates or missing names/states; no changes applied', invalid_count;
    END IF;
    SELECT count(*) INTO invalid_count FROM toll_csv_input
    WHERE NOT (longitude::double precision BETWEEN -180 AND 180)
       OR NOT (latitude::double precision BETWEEN -90 AND 90);
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'CSV has % rows with coordinates outside valid ranges; no changes applied', invalid_count;
    END IF;
END;
$$;

CREATE TEMP TABLE toll_csv_distinct ON COMMIT DROP AS
SELECT DISTINCT btrim(toll_name) AS name, btrim(geo_state) AS state,
       longitude::double precision AS longitude, latitude::double precision AS latitude
FROM toll_csv_input;
SELECT (SELECT count(*) FROM toll_csv_input) AS input_rows,
       count(*) AS distinct_rows,
       (SELECT count(*) FROM toll_csv_input) - count(*) AS duplicate_rows
FROM toll_csv_distinct;

LOCK TABLE toll_plaza IN EXCLUSIVE MODE;
DELETE FROM toll_plaza;
INSERT INTO toll_plaza(name, state, location)
SELECT name, state, ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
FROM toll_csv_distinct ORDER BY name, state, longitude, latitude;
COMMIT;
ANALYZE toll_plaza;
