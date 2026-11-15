# EC2 deployment with Neon

Run commands from the repository root on EC2. Requires Docker with Compose and the existing Neon schema and dataset. The production Compose file runs only the API; it performs no migration or import.

## Configuration

```sh
cp src/bootstrap/production.env.example src/bootstrap/.env.production
chmod 600 src/bootstrap/.env.production
```

Edit `.env.production` on EC2. Set the Neon JDBC URL, application username/password, and Google API key. Use a dedicated database role with SELECT access to `toll_plaza` and `toll_dataset_metadata`. Keep credentials outside the URL and retain TLS parameters. The file is gitignored and excluded from the Docker build context.

The existing Dockerfile is reused. Build on EC2 to use its native CPU architecture; if building on your Mac, explicitly target the EC2 architecture before transferring the image. No database image is needed.

## Validate and start

```sh
docker compose --env-file src/bootstrap/.env.production -f src/bootstrap/compose.production.yaml config --quiet
docker compose --env-file src/bootstrap/.env.production -f src/bootstrap/compose.production.yaml up --build -d
docker compose --env-file src/bootstrap/.env.production -f src/bootstrap/compose.production.yaml logs -f api
```

The build runs tests. Missing required credentials fail Compose validation. Logs rotate at 10 MB with three files per container.

The API binds to localhost:8080 on EC2 by default. Put an HTTPS reverse proxy on the host in front of it, or use an SSH tunnel for private testing. This setup does not configure public HTTPS, access controls, or EC2 security groups. Swagger remains enabled; limit access appropriately before public launch.

## Stop or update

```sh
docker compose --env-file src/bootstrap/.env.production -f src/bootstrap/compose.production.yaml down
```

After changing runtime credentials, recreate the API container:

```sh
docker compose --env-file src/bootstrap/.env.production -f src/bootstrap/compose.production.yaml up -d --no-build --force-recreate api
```

Stopping or recreating this stack does not manage Neon. Local development continues to use `src/bootstrap/compose.yaml` and its separate environment file.
