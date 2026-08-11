# Effective availability slots

`GET /api/v1/organizations/{organizationId}/availability/slots` requires `branchId`, `serviceId` and an ISO date; `specialistId` is optional. Access requires `SCHEDULE_VIEW`, `APPOINTMENTS_CREATE`, or platform administration.

The response includes the requested date, IDs, effective IANA timezone and explicit specialist/start/end slots. Start and end carry their UTC offset. No JPA entities are returned.

## Calculation

The service validates the active organization, branch, catalog service and branch-service assignment. It then selects active specialists assigned to both branch and service, active weekly windows effective on the date, and removes intervals affected by global/branch/specialist schedule blocks or capacity-consuming appointments.

Effective duration uses `specialist_services.custom_duration_minutes` first, then `services.duration_minutes`. Preparation and cleanup minutes reserve the conflict interval but are not added to the visual appointment duration. Standard overlap is `existingStart < requestedEnd AND existingEnd > requestedStart`; therefore adjacent visual intervals remain valid unless a buffer bridges them.

Slots advance by `AGENDAFLOW_SLOT_INTERVAL_MINUTES`, default `15`; the value must be positive. A 45-minute service can begin on any 15-minute boundary whose complete service and buffer interval fits.

Timezone is branch timezone when present, otherwise organization timezone. Weekly `TIME` values are interpreted in that zone; server timezone is never used as a scheduling source.
