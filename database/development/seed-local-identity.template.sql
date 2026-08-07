-- LOCAL DEVELOPMENT TEMPLATE ONLY. Never run this file in shared or production databases.
-- 1. Generate a BCrypt value with LocalPasswordHashTool.
-- 2. Replace every __PLACEHOLDER__ below before execution.
-- 3. Execute manually after Flyway has applied V1 and V2.
-- This file is intentionally outside src/main/resources/db/migration and is never run by Flyway.

BEGIN;

WITH inserted_organization AS (
    INSERT INTO agendaflow.organizations (legal_name, trade_name, tax_identifier, status)
    SELECT '__ORGANIZATION_LEGAL_NAME__', '__ORGANIZATION_TRADE_NAME__', '__UNIQUE_TAX_IDENTIFIER__', 'ACTIVE'
    WHERE NOT EXISTS (
        SELECT 1 FROM agendaflow.organizations
        WHERE tax_identifier = '__UNIQUE_TAX_IDENTIFIER__' AND deleted_at IS NULL
    )
    RETURNING id
), selected_organization AS (
    SELECT id FROM inserted_organization
    UNION ALL
    SELECT id FROM agendaflow.organizations
    WHERE tax_identifier = '__UNIQUE_TAX_IDENTIFIER__' AND deleted_at IS NULL
    LIMIT 1
), inserted_user AS (
    INSERT INTO agendaflow.app_users (email, password_hash, first_name, last_name)
    SELECT '__USER_EMAIL__', '__BCRYPT_HASH__', '__FIRST_NAME__', '__LAST_NAME__'
    WHERE NOT EXISTS (
        SELECT 1 FROM agendaflow.app_users WHERE email = '__USER_EMAIL__'
    )
    RETURNING id
), selected_user AS (
    SELECT id FROM inserted_user
    UNION ALL
    SELECT id FROM agendaflow.app_users WHERE email = '__USER_EMAIL__'
    LIMIT 1
), inserted_membership AS (
    INSERT INTO agendaflow.organization_memberships (organization_id, user_id, status)
    SELECT organization.id, app_user.id, 'ACTIVE'
    FROM selected_organization organization
    CROSS JOIN selected_user app_user
    ON CONFLICT (organization_id, user_id) DO NOTHING
    RETURNING id
), selected_membership AS (
    SELECT id FROM inserted_membership
    UNION ALL
    SELECT membership.id
    FROM agendaflow.organization_memberships membership
    JOIN selected_organization organization ON organization.id = membership.organization_id
    JOIN selected_user app_user ON app_user.id = membership.user_id
    LIMIT 1
)
INSERT INTO agendaflow.membership_roles (membership_id, role_id)
SELECT membership.id, role.id
FROM selected_membership membership
JOIN agendaflow.roles role
  ON role.organization_id IS NULL
 AND role.name = '__ROLE_NAME__'
 AND role.is_active = TRUE
ON CONFLICT (membership_id, role_id) DO NOTHING;

COMMIT;
