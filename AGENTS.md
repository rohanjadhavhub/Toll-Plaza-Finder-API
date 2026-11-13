# Toll Plaza Finder

Main instruction file for all AI agents (Claude, Codex, Gemini, etc.).
`CLAUDE.md` and `GEMINI.md` are symlinks to this file. Edit only `AGENTS.md`.
Keep agent instructions in this root file only. Do not create child `AGENTS.md` files.

## Project
- What it does: Finds toll plazas along Google's driving route between Indian PIN codes.
- Tech stack: Java 21, Maven, Spring Boot, Spring JDBC, PostgreSQL/PostGIS, JUnit 5, Swagger UI.

## Commands
- Build: `mvn package`
- Dev: `mvn spring-boot:run` (localhost:8080)
- Test: `mvn test` (no database or live Google calls)
- PostGIS tests: `mvn verify` requires explicit database approval and opt-in configuration.
- Local Docker: `docker compose --env-file src/bootstrap/.env -f src/bootstrap/compose.yaml up --build -d` (see `docs/development.md`; starting a database requires approval).

## Structure
- `.agents/skills/` — shared agent skills (`.claude/skills` is a symlink to it)
- `src/main/java/com/tollfinder/` — API, application, domain, routing, spatial, cache, and configuration packages
- `src/test/` — JUnit tests and dependency-free smoke checks
- `src/bootstrap/` — Dockerfile, Compose, build ignore rules, and local environment template
- `Dataset/toll_plaza_india.csv` — source plaza dataset
- `docs/` — project docs
- `docs/development.md` — setup, configuration, API usage, and limitations
- `docs/adr/` — architecture decisions (`0001-example-decision.md`)
- `docs/database/` — SQL migration files

## Guardrails (strict)
- Never touch any database (read, write, or migrate) until the User says so. Ask every time.
- Never commit, push, or open PRs on GitHub until the User says so. Ask every time.
- Never read, write, or run anything outside this project folder.

## Rules
- Keep code simple, readable, and modular.
- Do not add dependencies without the User's approval.
- Never commit `.env` files or secrets.
- Preserve screenshot-compatible JSON. Response distances use kilometers; internal calculations use meters.
- Keep HTTP mapping in `api`, orchestration in `application`, and immutable values in `domain`.
- Keep Google integration in `routing` and parameterized PostGIS queries in `spatial`.
- WGS84 coordinates use longitude before latitude. Meter-based spatial calculations use geography.
- Cache only dataset-derived plaza candidates, never Google routes or final route-specific responses.
- Never log API keys or geocoding URLs.
- Keep SQL manually applied and document what changed, why, how to apply, and how to verify.
- ADRs are written by the User. Remind them when consequential decisions need recording.
