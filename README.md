# AgendaFlow API

AgendaFlow API is the main REST backend for AgendaFlow, a multi-organization SaaS platform for managing organizations, branches, specialists, services, customers, schedules, and appointments.

## Status

**Phase 0 — Technical bootstrap.** This repository currently provides the application foundation, technical health tooling, and a guarded system-information endpoint. Business modules, JWT authentication, appointments, and multi-tenancy are not implemented yet.

## Backend responsibility

The API will expose the platform's REST interfaces, coordinate business use cases, enforce security, and persist AgendaFlow data in PostgreSQL as later phases introduce those capabilities.

## Stack

| Component | Version |
| --- | --- |
| Java | 21 |
| Maven Wrapper | 3.9.16 |
| Spring Boot | 4.0.7 |
| springdoc OpenAPI | 3.0.3 |
| PostgreSQL | JDBC driver managed by Spring Boot |
| Flyway | managed by Spring Boot |
| Tests | JUnit 5, Spring Boot Test, Spring Security Test, Testcontainers |

## Local requirements

- JDK 21
- PostgreSQL, for the `local` profile
- Internet access on the first Maven build, to download dependencies

## PostgreSQL configuration

The local profile expects PostgreSQL and validates the schema; Hibernate never creates or updates tables. Flyway is enabled only for the local profile and reads `classpath:db/migration`.

Default local connection values are:

```text
DB_URL=jdbc:postgresql://localhost:5432/agendaflow_db
DB_USERNAME=agendaflow_user
DB_PASSWORD=agendaflow_local_password
```

Copy `.env.example` only as a reference for your environment. Do not commit a real `.env` file.

## Environment variables

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC URL for PostgreSQL |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Reserved for a future JWT phase |
| `JWT_ISSUER` | Reserved for a future JWT phase |

## Maven Wrapper commands

```cmd
mvnw.cmd --version
mvnw.cmd clean test
mvnw.cmd clean verify
```

On Unix-like systems use `./mvnw clean verify`.

## Run locally

Start PostgreSQL with the configured database and schema migrations, then run:

```cmd
mvnw.cmd spring-boot:run
```

The default local profile listens on port `8080`.

## Tests

```cmd
mvnw.cmd clean test
```

Bootstrap tests use the `test` profile and explicitly disable Flyway, DataSource, JPA, and Spring Batch JDBC auto-configuration. They do not require a local PostgreSQL instance or start Testcontainers in this phase.

## Technical endpoints

| Endpoint | Purpose |
| --- | --- |
| `GET /api/v1/system/info` | Bootstrap service information |
| `/swagger-ui.html` | Swagger UI |
| `/v3/api-docs` | OpenAPI JSON |
| `/actuator/health` | Health status |

## Package structure

```text
com.flakomencia.agendaflow
├── common
│   ├── config
│   ├── exception
│   ├── persistence
│   ├── security
│   └── system
├── organization
├── branch
├── identity
├── customer
├── servicecatalog
├── specialist
├── scheduling
├── appointment
├── notification
└── reporting
```

Only the technical bootstrap configuration and system endpoint have classes in Phase 0. The remaining packages establish the intended module boundaries without placeholder business classes.

## Flyway strategy

Flyway owns all database schema changes. Existing migration files, when present, are preserved exactly as authored. This bootstrap neither executes migrations nor creates new ones. Production and local schema evolution must use versioned migrations under `src/main/resources/db/migration`.

All primary and foreign keys in the designed PostgreSQL schema use `BIGINT`; future Java persistence mappings must represent them with `Long`, never UUIDs unless the database design is explicitly changed in a future decision.

## Related projects

- [agendaflow-web](../agendaflow-web)
- [agendaflow-notification-service](../agendaflow-notification-service)

## Not implemented yet

- JPA entities and repositories
- Business DTOs, mappers, use cases, and services
- JWT, login, refresh tokens, users, and roles
- Multi-tenant rules and seed data
- Scheduling, appointments, and notifications
- Spring Batch jobs
