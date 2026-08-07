# Roles and permissions

Spring Security authorities are derived from the authenticated membership. Permission codes are
mapped directly (`ORGANIZATION_VIEW`, `BRANCHES_MANAGE`), while role names receive `ROLE_`
(`ROLE_PLATFORM_ADMIN`). Method security protects organization and branch operations.

| Operation | Required authority |
| --- | --- |
| Create organization | `ROLE_PLATFORM_ADMIN` |
| List organizations | `ORGANIZATION_VIEW` or platform administrator |
| View organization | `ORGANIZATION_VIEW` or platform administrator |
| Update organization | `ORGANIZATION_UPDATE` or platform administrator |
| List/view branches | `BRANCHES_VIEW` or platform administrator |
| Create/update branches | `BRANCHES_MANAGE` or platform administrator |

V1 defines the global roles, all 24 stable permission codes, and the operational role mappings. V2
completes the previously missing `PLATFORM_ADMIN` mapping by assigning every active permission.
Organization-specific roles are accepted only when their `organization_id` matches the active
membership organization.

Branch restrictions from `membership_branches` are not enforced in Phase 3.
