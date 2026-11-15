# Toll Plaza Finder

A Java Spring Boot API that finds toll plazas along a driving route between two Indian PIN codes. It uses Google Maps for routing and PostgreSQL/PostGIS to match plazas from a CSV dataset to the route.

[Swagger UI](https://toll-api.rohanjadhav.dev/swagger-ui.html) · [Development guide](docs/development.md) · [Production deployment](docs/production.md)

## Features

- Validates six-digit Indian PIN codes and handles invalid inputs and unavailable routes.
- Finds plazas within a configurable route corridor, 50 meters by default.
- Returns plazas in travel order with coordinates and estimated distance from the source.
- Caches dataset-derived plaza candidates for overlapping routes. Google routes and final responses are not cached.
- Includes OpenAPI documentation and JUnit tests.

## Stack

Java 21, Spring Boot, Maven, Spring JDBC, PostgreSQL 16, PostGIS, JUnit 5, and Docker. Production uses Neon PostgreSQL, with the API hosted on EC2 behind Nginx.

## Run locally

Install Docker with Compose, then run from the repository root:

```sh
cp src/bootstrap/.env.example src/bootstrap/.env
```

Set `DB_PASSWORD` and `GOOGLE_MAPS_API_KEY` in that file. Your Google project must have billing enabled and the Geocoding and Routes APIs enabled. Keep credentials out of Git.

```sh
docker compose --env-file src/bootstrap/.env -f src/bootstrap/compose.yaml up --build -d
```

Initialize the database and import the CSV using the [development guide](docs/development.md#database-initialization). Startup does not perform these steps. The import replaces existing plaza rows.

Open Swagger at http://localhost:8080/swagger-ui.html. The local database configuration targets ARM64 for Mac M1; see the development guide for other hosts.

## API

```http
POST /api/v1/toll-plazas
Content-Type: application/json
```

```json
{
  "sourcePincode": "110001",
  "destinationPincode": "560001"
}
```

Illustrative response:

```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "560001",
    "distanceInKm": 2100
  },
  "tollPlazas": [
    {
      "name": "Example Toll Plaza",
      "latitude": 28.7041,
      "longitude": 77.1025,
      "distanceFromSource": 200
    }
  ]
}
```

Both distances use kilometers. No matching plazas returns HTTP 200 with an empty `tollPlazas` array. See Swagger for error responses and examples. In Postman, use POST with a raw JSON body.

## Tests

With JDK 21+ and Maven installed:

```sh
mvn test
```

These tests use mocked dependencies and make no live Google or database calls. PostGIS integration tests require a separately approved disposable database and explicit opt-in; see the [test instructions](docs/development.md#running-without-docker).

## Repository layout

- `src/main/java/com/tollfinder/` — API, business logic, routing, spatial queries, and caching.
- `src/test/` — automated tests and dependency-free smoke checks.
- `src/bootstrap/` — Docker, environment templates, and Nginx configuration.
- `Dataset/` — source toll-plaza CSV.
- `docs/database/` — manually applied schema and import SQL.

## Limitations

Matching uses proximity to the route, so parallel roads and opposite carriageways can produce false matches. Plaza distances are estimates measured from the resolved source postal location. This API does not calculate toll prices.

## License

The project code is licensed under the [MIT License](LICENSE).
