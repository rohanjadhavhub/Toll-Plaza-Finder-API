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

The API binds to localhost:8080 on EC2. Production enables forwarded-header handling so redirects and Swagger recognize Nginx's external HTTPS address. Nginx replaces incoming forwarding headers; keep the API port private.

## HTTPS with Nginx

Point the DNS A record for `toll-api.rohanjadhav.dev` to the EC2 public IP, preferably an Elastic IP. Allow inbound TCP 80 and 443 in the EC2 security group and any active host firewall. Keep SSH restricted to your IP and port 8080 private. Only publish an AAAA record if IPv6 is configured end to end.

On Ubuntu, install the host tools (other operating systems need their own installation commands):

```sh
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx
sudo systemctl enable --now nginx
```

If UFW is already enabled, allow the `Nginx Full` profile. Do not enable a firewall without preserving SSH access.

After pulling these changes, recreate the API with the updated production Compose file. Keep `API_PORT=8080` to match the supplied Nginx upstream:

```sh
sudo docker compose --env-file src/bootstrap/.env.production -f src/bootstrap/compose.production.yaml up -d --no-build --force-recreate api
```

Install the HTTP bootstrap configuration once, then validate and reload Nginx:

```sh
sudo install -m 644 src/bootstrap/nginx/toll-api.conf /etc/nginx/conf.d/toll-api.conf
sudo nginx -t
sudo systemctl reload nginx
```

Confirm DNS resolves to EC2 and HTTP reaches the proxy before requesting the certificate:

```sh
curl -I http://toll-api.rohanjadhav.dev/v3/api-docs
sudo certbot --nginx -d toll-api.rohanjadhav.dev --redirect
sudo nginx -t
sudo certbot renew --dry-run
systemctl list-timers --all | grep certbot
```

Certbot adds HTTPS and redirects HTTP to HTTPS. Ensure automatic renewal is scheduled; the dry run alone does not schedule it. See [Certbot's Nginx instructions](https://certbot.eff.org/instructions?ws=nginx&os=pip).

Swagger: https://toll-api.rohanjadhav.dev/swagger-ui.html

API: `POST https://toll-api.rohanjadhav.dev/api/v1/toll-plazas`

The repository file is an HTTP bootstrap configuration. Do not copy it over the installed file after Certbot adds TLS settings. Preserve the server's certificate configuration during later updates. HTTPS does not add authentication or rate limiting; restrict access before exposing Google-quota-consuming endpoints publicly.

## Stop or update

```sh
docker compose --env-file src/bootstrap/.env.production -f src/bootstrap/compose.production.yaml down
```

After changing runtime credentials, recreate the API container:

```sh
docker compose --env-file src/bootstrap/.env.production -f src/bootstrap/compose.production.yaml up -d --no-build --force-recreate api
```

Stopping or recreating this stack does not manage Neon. Local development continues to use `src/bootstrap/compose.yaml` and its separate environment file.
