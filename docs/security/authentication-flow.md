# Authentication flow

`POST /api/v1/auth/login` requires email, password, and `organizationId`. Requiring the organization
is a deliberate Phase 3 simplification: one login creates one immutable tenant session. A later
phase may add an explicit, re-authenticated organization-switch flow.

The application normalizes the email, locks the user row for a transactional credential check,
uses BCrypt, and then validates user flags, membership status, and organization status. Unknown
email, bad password, blocked/inactive user, missing/inactive membership, and inactive organization
all return the same `401 INVALID_CREDENTIALS` response.

Bad passwords increment `failed_login_attempts`. At `AUTH_MAX_FAILED_ATTEMPTS` (default 5),
`is_locked` becomes true. A successful login resets the counter and updates `last_login_at`. Account
recovery and administrative unlock are intentionally deferred.

After validation, roles and permissions are loaded from PostgreSQL and embedded in a short-lived
user token. `GET /api/v1/auth/me` validates that token and reloads the membership, user,
organization, roles, and permissions; it does not rely only on frontend state.

Rate limiting, refresh, logout revocation, MFA, password recovery and public registration are not
implemented.
