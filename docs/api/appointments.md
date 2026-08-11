# Appointments API

Appointments are organization-scoped and use `BIGINT` identifiers mapped to Java `Long`. Their
instants are stored as `TIMESTAMPTZ`; responses render appointment times using the branch timezone
with the organization timezone as fallback.

## Routes and permissions

| Method and route | Authority |
| --- | --- |
| `POST /api/v1/organizations/{organizationId}/appointments` | `APPOINTMENTS_CREATE` |
| `GET /api/v1/organizations/{organizationId}/appointments` | `APPOINTMENTS_VIEW` |
| `GET /api/v1/organizations/{organizationId}/appointments/{appointmentId}` | `APPOINTMENTS_VIEW` |
| `GET .../{appointmentId}/history` | `APPOINTMENTS_VIEW` |
| `POST .../{appointmentId}/reschedule` | `APPOINTMENTS_UPDATE` |
| `POST .../{appointmentId}/cancel` | `APPOINTMENTS_CANCEL` |
| `POST .../{appointmentId}/confirm` | `APPOINTMENTS_UPDATE` |
| `POST .../{appointmentId}/check-in` | `APPOINTMENTS_UPDATE` |
| `POST .../{appointmentId}/start` | `APPOINTMENTS_UPDATE` |
| `POST .../{appointmentId}/complete` | `APPOINTMENTS_COMPLETE` |
| `POST .../{appointmentId}/no-show` | `APPOINTMENTS_COMPLETE` |

`PLATFORM_ADMIN` remains an alternative on protected actions. Resource lookup is tenant-scoped;
cross-tenant identifiers are not disclosed. Invalid state transitions and attempts to mark a
future appointment as no-show return `409` through the uniform `ApiErrorResponse` contract.

Create, reschedule and cancel each append history and may create a durable notification-outbox row
in the same PostgreSQL transaction. Confirm, check-in, start, complete and no-show never generate
notification events in Phase 6.
