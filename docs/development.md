# Development

## Local setup with Docker

Requires Docker with Compose. Docker files are in `src/bootstrap/`; no local Java or Maven installation is needed. Run commands from the repository root.

```sh
cp src/bootstrap/.env.example src/bootstrap/.env
```

Set `DB_PASSWORD` and `GOOGLE_MAPS_API_KEY` in the gitignored environment file. Enable Google's Geocoding and Routes APIs and set quota limits. The key is required for route searches, not Swagger or build tests.

```sh
docker compose --env-file src/bootstrap/.env -f src/bootstrap/compose.yaml up --build -d
```

This builds the API, runs tests, and starts PostgreSQL 16/PostGIS 3.6.1. The database runs natively on Mac M1 using a [community ARM64 image](https://github.com/ImreSamu/docker-postgis). For an AMD64 host, change the database platform in Compose to `linux/amd64`.

The API is at localhost:8080. Change `API_PORT` or `LOCAL_DB_PORT` in the environment file if needed. Containers communicate through `db:5432`.

## Database initialization

Obtain explicit approval before accessing any database. Startup does not apply the application schema or import the dataset.

Apply the schema once:

```sh
docker compose --env-file src/bootstrap/.env -f src/bootstrap/compose.yaml exec db \
  sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f docs/database/0001-toll-plaza-schema.sql'
```

Import `Dataset/toll_plaza_india.csv`. This replaces all existing plaza rows, validates the input, and removes exact duplicates:

```sh
docker compose --env-file src/bootstrap/.env -f src/bootstrap/compose.yaml exec db \
  sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f docs/database/0002-import-toll-plazas.sql'
```

The `postgres_arm64_data` volume persists across restarts. It is separate from any older PostGIS 3.7 volume; do not reuse that data directory with this older extension version. Changing `DB_PASSWORD` does not update an existing database's password.

## API documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

```sh
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H 'Content-Type: application/json' \
  -d '{"sourcePincode":"110001","destinationPincode":"560001"}'
```

Distances use kilometers. See Swagger for validation, response examples, and errors. “Try it out” uses real Google API quota.

## Logs, rebuilds, and shutdown

```sh
docker compose --env-file src/bootstrap/.env -f src/bootstrap/compose.yaml logs -f api
docker compose --env-file src/bootstrap/.env -f src/bootstrap/compose.yaml down
```

`down` preserves database data. For detailed build failures, run:

```sh
docker build --progress=plain -f src/bootstrap/Dockerfile .
```

## Running without Docker

Requires JDK 21+, Maven 3.6.3+, PostgreSQL 16+, and PostGIS 3.2+. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `GOOGLE_MAPS_API_KEY` in your shell. The application does not load `.env` files itself.

```sh
mvn test
mvn package
mvn spring-boot:run
```

`mvn test` runs JUnit tests without a database or live Google calls. `package` builds the executable JAR.

PostGIS integration tests require an explicitly approved disposable database with PostGIS installed in `public`:

```sh
export TOLL_FINDER_RUN_DB_TESTS=true
export TEST_DB_URL='jdbc:postgresql://localhost:5432/toll_finder_test'
export TEST_DB_USERNAME='toll_finder_test'
export TEST_DB_PASSWORD='your-test-password'
mvn verify
```

These tests create and remove a temporary schema. They are skipped without the opt-in variable.

## Operational notes

- The default 50-meter corridor can match parallel roads and opposite carriageways. Plaza distances are estimates from the resolved source postal location.
- Only dataset-derived plaza candidates are cached. Google routes and final responses are not cached; see [Google's policies](https://developers.google.com/maps/documentation/routes/policies).
- Dataset changes invalidate candidate caches through version metadata. Restart application instances after restoring an older database snapshot.
- For staging and production, use the same application Dockerfile with Neon connection settings. The Compose file is local-only. Use separate credentials for the application (SELECT access) and schema/import operations.
