# AgendaFlow API

Main REST backend for AgendaFlow, a multi-organization SaaS platform for reservations and
appointment management.

## Status

**Phase 6 - operational lifecycle and Transactional Outbox.** The API manages organizations,
branches, identity/RBAC, the service catalog, specialists, availability, customers, effective slots,
appointments, their operational lifecycle and durable notification submission.

## Stack

| Component | Version |
| --- | --- |
| Java | 21 |
| Maven Wrapper | 3.9.16 |
| Spring Boot | 4.0.7 |
| springdoc OpenAPI | 3.0.3 |
| PostgreSQL integration tests | 18.4 |
| Schema management | Flyway |
| Tests | JUnit, Mockito, MockMvc and Testcontainers |

All PostgreSQL primary and foreign keys are `BIGINT` and map to Java `Long`. Monetary values map to
`BigDecimal`; wall-clock hours use `LocalTime`, dates use `LocalDate`, and timestamps use
`OffsetDateTime`.

## Local configuration and commands

The default `local` profile uses PostgreSQL and the variables documented in `.env.example`.
User-token and service-token secrets must be different values of at least 32 UTF-8 bytes. Never
commit `.env` or real credentials. Hibernate uses `ddl-auto: validate`; Flyway alone owns the schema.

```cmd
mvnw.cmd --version
mvnw.cmd clean test
mvnw.cmd clean verify
mvnw.cmd spring-boot:run
```

`clean test` is database-free. `clean verify` starts disposable `postgres:18.4` containers and
applies Flyway V1/V2/V3 without using the local database; Docker is required. The API listens on
`http://localhost:8080`.

## Routes

| Area | Base route | Authorities |
| --- | --- | --- |
| Login/session | `/api/v1/auth` | Public login; Bearer session |
| Organizations | `/api/v1/organizations` | Organization authorities |
| Branches | `/api/v1/organizations/{organizationId}/branches` | `BRANCHES_VIEW/MANAGE` |
| Categories/services | `/api/v1/organizations/{organizationId}/service-categories`, `/services` | `SERVICES_VIEW/MANAGE` |
| Specialists | `/api/v1/organizations/{organizationId}/specialists` | `SPECIALISTS_VIEW/MANAGE` |
| Availability | `/api/v1/organizations/{organizationId}/availability/slots` | `SCHEDULE_VIEW` |
| Customers | `/api/v1/organizations/{organizationId}/customers` | `CUSTOMERS_VIEW/MANAGE` |
| Appointments | `/api/v1/organizations/{organizationId}/appointments` | Appointment authorities |
| OpenAPI | `/v3/api-docs`, `/swagger-ui.html` | Public technical documentation |
| Health | `/actuator/health` | Public technical health |

Paged endpoints return the stable `PageResponse` contract. API details are documented under
[`docs/api`](docs/api/README.md), including [customers](docs/api/customers.md),
[availability slots](docs/api/availability-slots.md), and [appointments](docs/api/appointments.md).

## Security, tenancy and booking

Every operational route is tenant-scoped in both the application and persistence layers;
cross-tenant resource identifiers resolve as `404`. `PLATFORM_ADMIN` retains its explicit bypass.
Passwords use BCrypt and user JWTs represent one active organization.

Booking locks the specialist pessimistically and recalculates effective capacity inside the
transaction. Appointment transitions follow `PENDING -> CONFIRMED -> CHECKED_IN -> IN_PROGRESS ->
COMPLETED`; eligible due appointments may become `NO_SHOW`. Reschedule retains the current business
status, and all successful lifecycle actions append immutable history.

## Transactional Outbox

Create, reschedule and cancel write an eligible email event in the same PostgreSQL transaction as
the appointment operation. Missing/invalid email or `email_consent != true` does not block booking
and creates no email event. Operational lifecycle actions do not emit notifications in Phase 6.

The configurable worker claims with `FOR UPDATE SKIP LOCKED`, performs HTTP outside the claim
transaction, retries with bounded exponential backoff, exhausts after the configured limit, and
recovers stale `PROCESSING` rows. It posts to the Quarkus notification service with a short-lived
service JWT containing only `notification:submit`; `notification:validate` remains separate.

See [appointment lifecycle](docs/architecture/appointment-lifecycle.md),
[transactional outbox](docs/architecture/transactional-outbox.md), and
[notification integration](docs/architecture/notification-integration.md).

## Flyway

V1 defines the base schema, V2 completes global `PLATFORM_ADMIN` permission associations, and V3
adds `agendaflow.notification_outbox`. Existing migrations are immutable. No migration creates
users, organizations, or operational seed data.

## Related projects

- [agendaflow-web](../agendaflow-web)
- [agendaflow-notification-service](../agendaflow-notification-service)

## Not implemented

- Waitlists, payments, reminders, recurring/group appointments or calendar synchronization.
- Refresh tokens, organization switching, user CRUD or membership-branch restrictions.
- Email-provider delivery in Spring, notification UI, Kafka, RabbitMQ, Redis or another broker.
- Docker packaging, cloud deployment or CI/CD.
