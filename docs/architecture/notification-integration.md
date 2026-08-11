# Notification integration

The publisher sends `POST /api/v1/notification-requests` to
`NOTIFICATION_SERVICE_BASE_URL` (default `http://localhost:8081`) using Spring `RestClient`. Only
`202 Accepted` is currently treated as durable acceptance. Timeouts, unexpected status codes,
authentication/authorization errors, and server errors remain retryable or eventually exhausted;
they are never marked `PUBLISHED`.

Each request gets a short-lived service JWT with:

- subject `agendaflow-api`;
- audience `agendaflow-notification-service`;
- `token_use=service`;
- group `notification:submit`.

This capability is separate from `notification:validate`, and there is no public token-issuance or
outbox CRUD endpoint. Spring does not send email. It only durably submits appointment notification
requests to the Quarkus boundary.

Email events require a syntactically valid customer email and explicit `email_consent=true`. Missing
or invalid email and refused consent never block the appointment; they simply produce no email
outbox row.
