# Authentication API

## Login

`POST /api/v1/auth/login` is public and consumes JSON:

```json
{
  "email": "user@example.com",
  "password": "local-password",
  "organizationId": 10
}
```

A successful response contains `accessToken`, `tokenType=Bearer`, `expiresIn`, `membershipId`,
`user`, `activeOrganization`, `roles`, and `permissions`. The organization ID is mandatory because
Phase 3 issues a token for exactly one tenant. Invalid identity details always return the same
`401 INVALID_CREDENTIALS`; the API never returns the password hash or internal account flags.

## Current session

`GET /api/v1/auth/me` requires `Authorization: Bearer <accessToken>` and returns current database
state for the authenticated user, active organization, roles and permissions.

Security failures use the standard JSON error body and the codes `AUTHENTICATION_REQUIRED`,
`INVALID_TOKEN`, `TOKEN_EXPIRED`, or `ACCESS_DENIED`. Swagger documents the `bearerAuth` scheme.
There are no refresh, registration, logout, password-reset, or organization-switch endpoints.
