-- Complete the global platform administrator role defined by V1.
-- This migration adds reference-data associations only; it creates no users or memberships.
INSERT INTO agendaflow.role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM agendaflow.roles role
CROSS JOIN agendaflow.permissions permission
WHERE role.organization_id IS NULL
  AND role.name = 'PLATFORM_ADMIN'
  AND role.is_active = TRUE
  AND permission.is_active = TRUE
ON CONFLICT (role_id, permission_id) DO NOTHING;
