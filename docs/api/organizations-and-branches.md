# Organizations and branches API

Phase 2 introduces the first business vertical slice. All identifiers are JSON numbers backed by
PostgreSQL `BIGINT` and Java `Long`.

## Endpoints

| Method | Path | Success |
| --- | --- | --- |
| `POST` | `/api/v1/organizations` | `201 Created` and `Location` |
| `GET` | `/api/v1/organizations` | `200 OK` paginated |
| `GET` | `/api/v1/organizations/{organizationId}` | `200 OK` |
| `PUT` | `/api/v1/organizations/{organizationId}` | `200 OK` |
| `POST` | `/api/v1/organizations/{organizationId}/branches` | `201 Created` and `Location` |
| `GET` | `/api/v1/organizations/{organizationId}/branches` | `200 OK` paginated |
| `GET` | `/api/v1/organizations/{organizationId}/branches/{branchId}` | `200 OK` |
| `PUT` | `/api/v1/organizations/{organizationId}/branches/{branchId}` | `200 OK` |

There is no DELETE operation in this phase.

## DTO and validation

Controllers accept explicit `OrganizationCreateRequest`, `OrganizationUpdateRequest`,
`BranchCreateRequest` and `BranchUpdateRequest` records. Responses use their corresponding response
records; JPA entities are never serialized.

`legalName` and branch `name` are required. String sizes follow V1 exactly. Email fields use email
validation, booking windows must be zero or positive, and coordinates are constrained to their
geographic ranges and `NUMERIC(9,6)` precision. Organization status accepts only `PENDING`,
`ACTIVE`, `SUSPENDED` or `INACTIVE`, matching the SQL check constraint.

PostgreSQL applies defaults for country, timezone, currency, language, booking flags/windows,
organization status and branch activity. Optional nullable columns remain optional.

## Pagination

Both collections accept Spring-style `page`, `size` and `sort` parameters. The default size is 20
and the configured maximum is 100. The response is stable and explicit:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Organization sort fields are `id`, `legalName`, `tradeName`, `status`, `createdAt` and `updatedAt`.
Branch sort fields are `id`, `name`, `code`, `active`, `createdAt` and `updatedAt`.

## Organization isolation

Every branch repository query used by the application includes `organizationId`. A request for a
real branch through a different organization returns `404 BRANCH_NOT_FOUND`; the application never
falls back to an unscoped branch lookup. A branch's organization cannot be changed by request DTOs.

## Error contract

Errors contain `timestamp`, HTTP `status`, stable `code`, safe `message`, request `path`, and
`fieldErrors` for request validation. Common codes include:

- `ORGANIZATION_NOT_FOUND`
- `BRANCH_NOT_FOUND`
- `ORGANIZATION_TAX_IDENTIFIER_EXISTS`
- `BRANCH_NAME_EXISTS`
- `BRANCH_CODE_EXISTS`
- `VALIDATION_ERROR`
- `DATA_INTEGRITY_CONFLICT`
- `INTERNAL_ERROR`

SQL, PostgreSQL details, stack traces and internal class names are never returned.

## Temporary access and CORS

These endpoints are temporarily public so frontend development can proceed before JWT exists. This
configuration is not production authorization. Development CORS permits `GET`, `POST`, `PUT` and
`OPTIONS` from `${APP_CORS_ALLOWED_ORIGINS:http://localhost:4200}` without credentials. The
`Location` response header is exposed to browser clients.
