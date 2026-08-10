# AgendaFlow API

Main REST backend for AgendaFlow, a multi-organization SaaS platform for reservations and
appointment management.

## Status

**Phase 4 — services, specialists and availability.** The API now manages organizations, branches,
the service catalog, specialist assignments, recurring availability rules and schedule blocks.
These rules are prerequisites for appointments; they do not generate bookable slots or reserve
appointments.

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

All PostgreSQL primary and foreign keys are `BIGINT` and map to Java `Long`. Monetary values map to
`BigDecimal`; wall-clock hours use `LocalTime`, validity dates use `LocalDate`, and timestamps use
`OffsetDateTime`.

## Local configuration

The default `local` profile uses PostgreSQL and the variables documented in `.env.example`.
User-token and service-token secrets must be different, strong values of at least 32 UTF-8 bytes.
Never commit `.env` or real credentials. Hibernate uses `ddl-auto: validate`; Flyway alone owns the
schema.

```cmd
mvnw.cmd --version
mvnw.cmd clean test
mvnw.cmd clean verify
mvnw.cmd spring-boot:run
```

`clean test` is database-free. `clean verify` starts disposable `postgres:18.4` containers, applies
Flyway V1/V2 and runs HTTP/persistence integrations without using the local database. Docker is
required for `verify`. The API listens on `http://localhost:8080`.

## Technical and business routes

| Area | Base route | Authorities |
| --- | --- | --- |
| Login/session | `/api/v1/auth` | Public login; Bearer session |
| Organizations | `/api/v1/organizations` | Organization authorities |
| Branches | `/api/v1/organizations/{organizationId}/branches` | `BRANCHES_VIEW/MANAGE` |
| Categories/services | `/api/v1/organizations/{organizationId}/service-categories`, `/services` | `SERVICES_VIEW/MANAGE` |
| Branch services | `/api/v1/organizations/{organizationId}/branches/{branchId}/services` | `SERVICES_VIEW/MANAGE` |
| Specialists | `/api/v1/organizations/{organizationId}/specialists` | `SPECIALISTS_VIEW/MANAGE` |
| Availability/blocks | `/api/v1/organizations/{organizationId}/specialists/{specialistId}` | `SCHEDULE_VIEW/MANAGE` |
| OpenAPI | `/v3/api-docs`, `/swagger-ui.html` | Public technical documentation |
| Health | `/actuator/health` | Public technical health |

Paged endpoints return the stable `PageResponse` contract rather than serializing Spring
`PageImpl`. Services, specialists and schedule blocks are paged; categories and assignment lists are
small organization-scoped lists.

API details:

- [Authentication](docs/api/authentication.md)
- [Organizations and branches](docs/api/organizations-and-branches.md)
- [Services](docs/api/services.md)
- [Specialists](docs/api/specialists.md)
- [Availability](docs/api/availability.md)

## Security and tenancy

The Phase 3 JWT contract is unchanged. Every operational route compares the path `organizationId`
with `AuthenticatedOrganizationContext`; `PLATFORM_ADMIN` may address another organization. The
application layer repeats this boundary and repositories include the organization in sensitive
lookups. Cross-tenant resource identifiers resolve as `404`.

Passwords remain BCrypt hashes. User access tokens last 30 minutes by default and contain one active
organization. The internal service-token issuer makes no HTTP call to Quarkus.

## Service and scheduling model

Categories and services belong directly to an organization. Explicit bridge entities hold
branch-service, specialist-branch and specialist-service configuration. Specialists may optionally
reference an active organization member.

Availability is a recurring rule for one specialist, branch and weekday (`0` through `6`), with a
time interval and optional validity dates. Active rules with intersecting validity dates cannot have
overlapping times. Schedule blocks represent the V1 types `BREAK`, `VACATION`, `HOLIDAY`,
`SICK_LEAVE`, `PERSONAL`, `MEETING`, `MAINTENANCE` and `OTHER`.

See [scheduling domain](docs/architecture/scheduling-domain.md) and
[backend modules](docs/architecture/backend-modules.md).

## Flyway

V1 defines the complete schema and initial role/permission reference data. V2 completes global
`PLATFORM_ADMIN` permission associations. Phase 4 does not add or change a migration. No users,
organizations or operational seed data are created.

## Related projects

- [agendaflow-web](../agendaflow-web)
- [agendaflow-notification-service](../agendaflow-notification-service)

## Not implemented

- Customers, appointments, waitlists, status history or final slot generation.
- Booking concurrency, payments or appointment reservations.
- Refresh tokens, organization switching, user CRUD or membership-branch restrictions.
- Notification delivery, Quarkus HTTP integration, asynchronous messaging or batch jobs.
- Docker packaging, cloud deployment or CI/CD.
