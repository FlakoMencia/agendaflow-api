# Multi-tenancy

AgendaFlow uses application-enforced row isolation in Phase 3. The validated user JWT supplies
`userId`, `membershipId`, and one `organizationId`; no custom request header can choose the tenant.
`AuthenticatedOrganizationContext` exposes these immutable IDs plus roles and permissions.

Organization and branch application services enforce the tenant again below the controller.
Regular users can see only their active organization. Branch repositories continue querying with
both `organizationId` and `branchId`. A cross-tenant identifier returns `404` so the API does not
confirm that another tenant's resource exists. `PLATFORM_ADMIN` is the explicit global bypass.

The database remains the authority for membership relationships. `/auth/me` revalidates current
state, while normal resource requests use the short-lived signed context. Database row-level
security, organization switching, membership-branch restrictions, and broader tenant-aware modules
are deferred.
