# AgendaFlow API

Main REST backend for AgendaFlow, a multi-organization SaaS platform for reservations and
appointment management.

## Status

**Phase 3 — identity, multi-tenancy and JWT authentication.** The API authenticates active users
inside one organization, protects the Phase 2 organization/branch vertical slice, and can issue a
short-lived internal notification-service token. Refresh tokens and business modules remain out of
scope.

## Stack

| Component | Version |
| --- | --- |
| Java | 21 |
| Maven Wrapper | 3.9.16 |
| Spring Boot | 4.0.7 |
| Spring Security / Resource Server | Managed by Spring Boot |
| springdoc OpenAPI | 3.0.3 |
| PostgreSQL integration tests | 18.4 |
| Schema management | Flyway |
| Tests | JUnit, Mockito, MockMvc and Testcontainers |

All database primary and foreign keys are PostgreSQL `BIGINT` mapped to Java `Long`.

## Local configuration

The default `local` profile uses PostgreSQL and the variables documented in `.env.example`:

```text
DB_URL=jdbc:postgresql://localhost:5432/agendaflow_db
DB_USERNAME=agendaflow_user
DB_PASSWORD=change-me
JWT_SECRET=change-me
JWT_ISSUER=agendaflow-api
JWT_AUDIENCE=agendaflow-web
JWT_ACCESS_TOKEN_TTL=30m
SERVICE_JWT_SECRET=change-me
SERVICE_JWT_ISSUER=agendaflow-api
SERVICE_JWT_AUDIENCE=agendaflow-notification-service
SERVICE_JWT_TTL=5m
AUTH_MAX_FAILED_ATTEMPTS=5
```

Use strong, different user/service secrets of at least 32 UTF-8 bytes. Do not commit `.env` or real
credentials. Non-local/test profiles reject repository placeholders. Hibernate uses
`ddl-auto: validate` and never creates or updates tables.

## Build, tests and execution

```cmd
mvnw.cmd --version
mvnw.cmd clean test
mvnw.cmd clean verify
mvnw.cmd spring-boot:run
```

`clean test` runs database-free bootstrap and unit tests. `clean verify` additionally starts
disposable `postgres:18.4` containers, applies Flyway V1/V2, and runs the HTTP security and
persistence integrations. Docker is required for `verify`; the local database is not used.

The API listens on `http://localhost:8080` by default.

## HTTP routes

| Route | Access and purpose |
| --- | --- |
| `POST /api/v1/auth/login` | Public; authenticate email/password for one `organizationId` |
| `GET /api/v1/auth/me` | Bearer; current user, active organization, roles and permissions |
| `GET /api/v1/system/info` | Public technical information |
| `/api/v1/organizations/**` | Bearer plus organization authorities |
| `/api/v1/organizations/{organizationId}/branches/**` | Bearer plus branch authorities and tenant match |
| `/swagger-ui.html` | Public development Swagger UI |
| `/v3/api-docs` | Public development OpenAPI JSON |
| `/actuator/health` | Public health status |

See [authentication](docs/api/authentication.md) and
[organizations and branches](docs/api/organizations-and-branches.md).

## Security and tenancy

Passwords are BCrypt hashes. Login uses a generic invalid-credential response and locks an account
after a configurable number of bad password attempts. A successful login resets that counter.

The user token lasts 30 minutes by default and has no refresh token. It fixes one active organization
chosen during login; this required field is a temporary Phase 3 contract until an explicit secure
organization-switch experience is designed. Permissions become direct authorities and roles become
`ROLE_*` authorities. Cross-tenant organization and branch identifiers return `404`.

The separate service-token issuer is internal only and currently makes no call to Quarkus. Details:

- [JWT design](docs/security/jwt-design.md)
- [Authentication flow](docs/security/authentication-flow.md)
- [Roles and permissions](docs/security/roles-and-permissions.md)
- [Multi-tenancy](docs/architecture/multi-tenancy.md)

CORS allows configured origins, `Authorization` and `Content-Type`, and `GET`, `POST`, `PUT`, and
`OPTIONS`, without cookies or credentialed wildcard origins.

## Flyway and local identity

Flyway is the sole source of schema and security reference data. `V1__initial_schema.sql` remains
unchanged; V2 only completes the global `PLATFORM_ADMIN` permission associations. Neither migration
creates users, memberships or organizations.

For manual local identity setup, follow
[local authentication testing](docs/security/local-authentication-testing.md). The template lives at
`database/development/seed-local-identity.template.sql`, outside Flyway, and contains placeholders
only.

## Related projects

- [agendaflow-web](../agendaflow-web)
- [agendaflow-notification-service](../agendaflow-notification-service)

## Not implemented

- Refresh tokens, token revocation, server logout or organization switching.
- Registration, invitations, email verification, password recovery, MFA or user/role CRUD.
- Rate limiting or `membership_branches` authorization.
- Services, specialists, customers, schedules or appointments.
- Calls to Quarkus, asynchronous messaging, deployment or CI/CD.
