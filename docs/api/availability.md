# Availability API

Availability routes are scoped to
`/api/v1/organizations/{organizationId}/specialists/{specialistId}`. Reads require `SCHEDULE_VIEW`;
mutations require `SCHEDULE_MANAGE`.

## Recurring availability

- `GET|POST .../availability`
- `PUT .../availability/{scheduleId}`

Each rule contains a required branch, weekday `0..6`, `startTime`, `endTime`, optional `validFrom`
and `validUntil`, and active state. Start must be before end and validity dates must be ordered.
Exact active duplicates return `RESOURCE_CONFLICT`; intersecting active time/date ranges for the same
organization, specialist, branch and weekday return `SCHEDULE_OVERLAP`.

## Schedule blocks

- `POST|GET .../schedule-blocks`
- `GET|PUT .../schedule-blocks/{blockId}`

Blocks use only V1 types and require `startsAt < endsAt`. Branch is optional in V1 but, when supplied,
must belong to the organization. Listings use the stable paged response.

These contracts administer scheduling rules and exceptions. They do not calculate slots, inspect
appointments or reserve capacity.
