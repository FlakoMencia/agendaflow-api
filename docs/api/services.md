# Service catalog API

All routes require Bearer JWT and an `organizationId` matching the active JWT tenant, except for a
`PLATFORM_ADMIN`. Cross-tenant identifiers return `404`.

## Categories

- `POST /api/v1/organizations/{organizationId}/service-categories` — `SERVICES_MANAGE`
- `GET /api/v1/organizations/{organizationId}/service-categories` — `SERVICES_VIEW`
- `GET /api/v1/organizations/{organizationId}/service-categories/{categoryId}` — `SERVICES_VIEW`
- `PUT /api/v1/organizations/{organizationId}/service-categories/{categoryId}` — `SERVICES_MANAGE`

Category names are unique within an organization. Categories are returned as an ordered list because
their expected cardinality is small.

## Services and branch assignments

- `POST|GET /api/v1/organizations/{organizationId}/services`
- `GET|PUT /api/v1/organizations/{organizationId}/services/{serviceId}`
- `GET /api/v1/organizations/{organizationId}/branches/{branchId}/services`
- `PUT /api/v1/organizations/{organizationId}/branches/{branchId}/services/{serviceId}`

Reads require `SERVICES_VIEW`; mutations require `SERVICES_MANAGE`. Services support the V1 fields
for category, duration, preparation/cleanup buffers, price, currency, approval, online booking,
active state and color. Prices are decimals and duration is positive. Service listings are paged.
The branch `PUT` is idempotent create-or-update and verifies both resources belong to the tenant.

There are no DELETE operations in Phase 4.
