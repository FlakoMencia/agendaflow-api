# JWT design

Phase 3 uses short-lived HMAC SHA-256 JWTs validated by Spring Security Resource Server. AgendaFlow
does not act as a full OAuth2 Authorization Server and does not issue refresh tokens.

## User access token

The user token is signed with `JWT_SECRET`, issued by `JWT_ISSUER` (default `agendaflow-api`), is
intended for `JWT_AUDIENCE` (default `agendaflow-web`), and lasts 30 minutes by default. It contains
only `sub`, `user_id`, `membership_id`, `organization_id`, `roles`, `permissions`, `token_use=user`,
`iss`, `aud`, `iat`, `nbf`, `exp`, and `jti`. The tenant is fixed for the token lifetime.

## Internal service token

`ServiceAccessTokenService` creates an internal token for later notification-service integration. It
uses a different key and configuration: `SERVICE_JWT_SECRET`, `SERVICE_JWT_ISSUER`,
`SERVICE_JWT_AUDIENCE`, and `SERVICE_JWT_TTL`. Its claims are `sub=agendaflow-api`,
`groups=[notification:validate]`, `token_use=service`, plus the registered time, issuer, audience and
identifier claims. There is no HTTP endpoint that exposes this token and Phase 3 makes no Quarkus
call.

Both secrets must differ and contain at least 32 UTF-8 bytes. Placeholder secrets are accepted only
for local/test profiles; another profile fails startup until secure external values are provided.
Secrets and token values must never be logged or committed.
