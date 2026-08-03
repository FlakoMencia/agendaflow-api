# Backend modules

AgendaFlow uses feature-oriented modules under `com.flakomencia.agendaflow`. Phase 2 implements only
`organization` and `branch`; cross-cutting HTTP contracts remain under `common`.

```text
organization/                 branch/
├── api                       ├── api
├── application               ├── application
├── domain                    ├── domain
└── infrastructure            └── infrastructure
```

- `api` owns REST controllers and request/response records.
- `application` owns transactional services, mappers and use-case errors.
- `domain` owns JPA entities and SQL-constrained enums.
- `infrastructure` owns Spring Data repositories.

The entities map the existing `agendaflow` schema; Flyway remains the only schema owner. Identity
`BIGINT` columns use `Long` and `GenerationType.IDENTITY`. Database defaults and the `updated_at`
trigger are preserved instead of recreated as Java lifecycle callbacks.

`Branch` has one lazy, unidirectional `ManyToOne` reference to `Organization`. `Organization` has no
branch collection, cascading or security rules. Entity equality considers only a non-null identity
and is safe for Hibernate proxies.

Branch services and repositories always combine branch and organization identifiers. This is the
first isolation boundary; authenticated membership and permission enforcement remain future work.
