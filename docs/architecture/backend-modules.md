# Backend modules

AgendaFlow uses feature-oriented modules under `com.flakomencia.agendaflow`. Implemented modules are
`organization`, `branch`, `identity`, `servicecatalog`, `specialist` and `scheduling`.

Each feature uses only the layers it needs:

```text
feature/
├── api/             REST controllers and validated request/response records
├── application/     transactional use cases, isolation rules and use-case errors
├── domain/          JPA entities, composite IDs and SQL-constrained enums
└── infrastructure/  organization-scoped Spring Data repositories
```

Cross-cutting HTTP contracts, errors, configuration and tenant security remain under `common`.
Flyway is the only schema owner. Identity `BIGINT` columns use `Long`; database defaults and the V1
`updated_at` trigger remain authoritative.

Associations are explicit entities because their tables carry state: `branch_services.is_active`,
`specialist_branches.is_primary/is_active` and specialist service overrides. Their composite keys do
not hide tenant checks: application services load both sides through organization-scoped queries.

Relations are lazy and unidirectional. There are no eager collections, indiscriminate cascades or
entity serialization. Appointment, customer and notification modules remain unimplemented.
