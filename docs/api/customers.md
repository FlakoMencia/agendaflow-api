# Customers API

Fase 5 exposes organization-scoped customer creation, pagination, detail and update. There is no delete or automatic merge.

## SQL contract

`customers.id` and `organization_id` are `BIGINT`; the latter is a required FK. Required fields are `first_name`, `last_name`, `country_code`, consent flags, `is_active`, `created_at` and `updated_at`. Email, telephone fields, date of birth, addresses, emergency contact, acceptance timestamps and internal notes are nullable. `country_code`, consents, activity and timestamps have SQL defaults. `deleted_at` is the soft-delete marker.

`preferred_contact_method` accepts only `EMAIL`, `SMS`, `PHONE`, `NONE` or null. `(organization_id, customer_number)` is unique. Tenant/name, tenant/email and tenant/phone indexes support lookup. Email remains optional and is only safely normalized to trimmed lowercase; no fuzzy deduplication is performed.

## Endpoints and permissions

- `POST /api/v1/organizations/{organizationId}/customers` — `CUSTOMERS_CREATE`.
- `GET /api/v1/organizations/{organizationId}/customers` — `CUSTOMERS_VIEW`, paginated.
- `GET /api/v1/organizations/{organizationId}/customers/{customerId}` — `CUSTOMERS_VIEW`.
- `PUT /api/v1/organizations/{organizationId}/customers/{customerId}` — `CUSTOMERS_UPDATE`.

Cross-tenant identifiers return `404`; invalid input returns `400`; duplicate non-null customer numbers return `409`.
