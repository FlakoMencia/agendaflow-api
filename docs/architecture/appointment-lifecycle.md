# Appointment lifecycle

The application policy is the single authority for allowed transitions:

```text
PENDING -> CONFIRMED -> CHECKED_IN -> IN_PROGRESS -> COMPLETED
```

`PENDING`, `CONFIRMED`, or `CHECKED_IN` may become `NO_SHOW`, but only when the scheduled instant
has arrived. `OffsetDateTime` comparison evaluates the instant, so differing valid offsets do not
change the decision. Reschedule retains its Phase 5 meaning: `PENDING` and `CONFIRMED` may be moved
without changing status, while an append-only history event records the operation. Cancellation is
allowed from `PENDING` and `CONFIRMED`.

Every successful operation locks the appointment, records the previous and new state, the database
timestamp, current actor when available, and a reason where appropriate. History is never updated or
deleted. The lifecycle timestamp columns (`confirmed_at`, `checked_in_at`, `started_service_at`, and
`completed_at`) are populated as their transitions occur.

Only create, reschedule, and cancel are notification-worthy in Phase 6. Operational transitions do
not enqueue events.
