# Transactional outbox

Flyway V3 adds `agendaflow.notification_outbox`. The same Spring transaction persists both the
appointment operation/history and its outbox event. A serialization or outbox insert failure rolls
back the complete booking operation; no HTTP call occurs while that transaction is open.

Rows use a `BIGINT` identity that is also the durable `eventId`. Stable event codes are
`APPOINTMENT_CREATED`, `APPOINTMENT_RESCHEDULED`, and `APPOINTMENT_CANCELLED`. Payloads contain only
the organization and appointment IDs, event kind, eligible email recipient, locale, occurrence
instant, and template variables. No user internals, secrets, or complete entities are serialized.

The scheduled worker performs a short PostgreSQL claim transaction with `FOR UPDATE SKIP LOCKED`,
marks each row `PROCESSING`, increments its attempt count, and commits before HTTP. This lets multiple
API instances partition work without claiming one row concurrently. The transport is at-least-once:
the notification service must use `eventId` as its idempotency key because a process can fail after a
remote acceptance but before the local `PUBLISHED` update.

Failures return to `PENDING` with bounded exponential backoff. After the configured maximum attempts
they become `EXHAUSTED`. A configurable timeout recovers stale `PROCESSING` claims after a crashed
worker. Logs identify the outbox/appointment/event and attempt, never the recipient, payload, JWT, or
secrets.
