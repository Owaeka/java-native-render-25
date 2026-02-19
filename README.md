# java-native-render-25

A template for deploying a Spring Boot 4 native image on [Render](https://render.com). It's a multi-tenant auth service that sits in front of Keycloak — your frontends talk to this API, and it handles the Keycloak stuff behind the scenes.

## Stack

- Java 25 + Spring Boot 4.0
- GraalVM native image (no JVM at runtime)
- Redis for caching and rate limiting
- Keycloak for identity (register, login, refresh, logout)
- Swagger UI via SpringDoc

## How it works

Each tenant is configured through environment variables (`TENANT_1_KEY`, `TENANT_1_REALM`, etc.). Requests include a tenant API key, and the service routes auth operations to the right Keycloak realm.

**Endpoints** — all under `/api/v1/auth`:

| Method | Path       | Description              |
|--------|------------|--------------------------|
| POST   | /register  | Create a new user        |
| POST   | /login     | Get access + refresh token |
| POST   | /refresh   | Refresh an access token  |
| POST   | /logout    | Revoke a refresh token   |

## Running locally

```bash
cp .env.example .env
# fill in your Keycloak and tenant values

docker-compose up
```

This starts Redis + the app. The app builds a native binary inside Docker (takes a few minutes the first time).

## Deploy to Render

The deployment flow:

1. Push to `main`
2. GitHub Actions builds the native image and pushes it to GHCR (`ghcr.io/<owner>/java-native-render-25`)
3. On Render, create a **Web Service** that pulls the pre-built image from GHCR

This avoids building the native image on Render (which would take too long and too much memory).

## Environment variables

See [`.env.example`](.env.example) for the full list. The main ones:

| Variable | Description |
|----------|-------------|
| `KEYCLOAK_URL` | Your Keycloak base URL |
| `KEYCLOAK_ADMIN_CLIENT_ID` | Admin client ID for user management |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | Admin client secret |
| `TENANT_n_KEY` | API key for tenant n |
| `TENANT_n_REALM` | Keycloak realm for tenant n |
| `TENANT_n_CLIENT_ID` | Client ID for tenant n |
| `TENANT_n_CLIENT_SECRET` | Client secret for tenant n |
| `REDIS_HOST` / `REDIS_PORT` | Redis connection |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
