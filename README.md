# AgendaFlow API

Main REST backend for AgendaFlow, a multi-organization SaaS platform for reservations and appointment
management.

## Status

**Phase 2 — Organizations and branches vertical slice.** Organizations and their organization-scoped
branches now work end to end through REST, validation, application services, JPA and PostgreSQL.
JWT, memberships, roles and production authorization are not implemented.

## Stack

| Component | Version |
| --- | --- |
| Java | 21 |
| Maven Wrapper | 3.9.16 |
| Spring Boot | 4.0.7 |
| springdoc OpenAPI | 3.0.3 |
| PostgreSQL integration test | 18.4 |
| Schema management | Flyway |
| Tests | JUnit, Mockito, MockMvc and Testcontainers |

## Local configuration

The default `local` profile expects PostgreSQL and uses:

```text
DB_URL=jdbc:postgresql://localhost:5432/agendaflow_db
DB_USERNAME=agendaflow_user
DB_PASSWORD=agendaflow_local_password
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200
```

Use `.env.example` as documentation only. Do not commit a real `.env`. Hibernate uses
`ddl-auto: validate`; it never creates or updates tables.

## Build, tests and execution

```cmd
mvnw.cmd --version
mvnw.cmd clean test
mvnw.cmd clean verify
mvnw.cmd spring-boot:run
```

`clean test` runs unit and database-free bootstrap tests. `clean verify` additionally runs the
integration suite against disposable `postgres:18.4` containers, applies V1 through Flyway and uses
real JPA repositories. Docker must be available for `verify`; the local database is never used by
integration tests.

The API listens on `http://localhost:8080` by default.

## API

| Endpoint | Purpose |
| --- | --- |
| `GET /api/v1/system/info` | Technical service information |
| `/api/v1/organizations` | Create and page organizations |
| `/api/v1/organizations/{organizationId}` | Get or update an organization |
| `/api/v1/organizations/{organizationId}/branches` | Create and page scoped branches |
| `/api/v1/organizations/{organizationId}/branches/{branchId}` | Get or update a scoped branch |
| `/swagger-ui.html` | Swagger UI |
| `/v3/api-docs` | OpenAPI JSON |
| `/actuator/health` | Health status |

Creation returns `201` with `Location`; reads and updates return `200`. Validation returns `400`,
missing resources `404`, and known uniqueness/integrity conflicts `409`. No DELETE endpoint exists.
See the full [organizations and branches contract](docs/api/organizations-and-branches.md).

## Pagination and isolation

Organization and branch lists support `page`, `size` and `sort`, with a maximum size of 100 and an
explicit page response. Every application query for a branch includes its `organizationId`; using a
branch ID through another organization returns 404.

## Security and CORS

Spring Security is present, but the new endpoints are temporarily permitted without authentication
for local development. This is not production-ready security. There are no in-memory users, Basic
Auth, login forms, JWTs or generated passwords.

CORS defaults to `http://localhost:4200`, is configurable through `APP_CORS_ALLOWED_ORIGINS`, allows
only `GET`, `POST`, `PUT` and `OPTIONS`, exposes `Location`, and does not enable credentials.

## Package architecture

`organization` and `branch` each contain `api`, `application`, `domain` and `infrastructure` layers.
DTOs are Java records, mapping is manual, and entities never cross the controller boundary. Details
are documented in [backend modules](docs/architecture/backend-modules.md).

## Flyway and identifiers

Flyway is the sole source of database schema changes. Phase 2 adds no migration and leaves
`V1__initial_schema.sql` unchanged. PostgreSQL defaults and triggers continue to own default values
and `updated_at` behavior.

All primary and foreign keys are `BIGINT` and map to Java `Long`; no UUID identifier was introduced.

## Related projects

- [agendaflow-web](../agendaflow-web)
- [agendaflow-notification-service](../agendaflow-notification-service)

## Not implemented

- AppUser, memberships, roles, permissions or tenant authorization.
- JWT, login, refresh tokens or service authentication.
- Services, specialists, customers, schedules or appointments.
- Soft-delete endpoints or automatic audit capture.
- Spring Batch jobs, Quarkus integration, deployment or CI/CD.
