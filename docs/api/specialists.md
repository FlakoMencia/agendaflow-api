# Specialists API

- `POST|GET /api/v1/organizations/{organizationId}/specialists`
- `GET|PUT /api/v1/organizations/{organizationId}/specialists/{specialistId}`
- `GET /api/v1/organizations/{organizationId}/specialists/{specialistId}/branches`
- `PUT /api/v1/organizations/{organizationId}/specialists/{specialistId}/branches/{branchId}`
- `GET /api/v1/organizations/{organizationId}/specialists/{specialistId}/services`
- `PUT /api/v1/organizations/{organizationId}/specialists/{specialistId}/services/{serviceId}`

Reads require `SPECIALISTS_VIEW`; mutations and assignments require `SPECIALISTS_MANAGE`.
Specialists are paged. A specialist can optionally reference an `app_user`, but that user must be an
active membership of the same organization and cannot be linked twice within that organization.

Branch assignments preserve `is_primary` and `is_active`. Service assignments preserve optional
custom duration and price plus `is_active`. Every side of an association is independently loaded by
organization, so foreign-tenant IDs return `404`. No DELETE operation is exposed.
