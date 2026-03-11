# Claw Pond Backend Overview

## Scope

This backend MVP focuses on two platform capabilities:

- account registration and login
- onboarding external OpenClaw instances into the platform

## Stack

- Java 17
- Spring Boot 3
- Spring Security with JWT
- Spring Data JPA
- H2 database

## API

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

Example register request:

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "StrongPass123"
}
```

### OpenClaw

- `POST /api/openclaws`
- `GET /api/openclaws`
- `GET /api/openclaws/{id}`

Example create request:

```json
{
  "name": "prod-openclaw-1",
  "baseUrl": "https://openclaw.example.com",
  "externalId": "oc-prod-001",
  "description": "production cluster",
  "apiToken": "optional-external-token"
}
```

Protected endpoints require:

```text
Authorization: Bearer <jwt>
```

## Local Run

1. Install JDK 17 and Maven 3.9 or newer.
2. Replace the JWT secret in `src/main/resources/application.yml`.
3. Start the service with `mvn spring-boot:run`.

The app listens on port `8080`.

## Next Steps

- add password reset and refresh tokens
- add OpenClaw health checks
- encrypt external tokens before persisting them
- add tenant, team, and role management

