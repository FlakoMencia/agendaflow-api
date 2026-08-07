package com.flakomencia.agendaflow.identity.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flakomencia.agendaflow.identity.domain.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    @Query(value = """
            SELECT DISTINCT permission.*
            FROM agendaflow.permissions permission
            JOIN agendaflow.role_permissions role_permission ON role_permission.permission_id = permission.id
            JOIN agendaflow.roles role ON role.id = role_permission.role_id
            JOIN agendaflow.membership_roles membership_role ON membership_role.role_id = role.id
            WHERE membership_role.membership_id = :membershipId
              AND permission.is_active = TRUE
              AND role.is_active = TRUE
              AND (role.organization_id IS NULL OR role.organization_id = :organizationId)
            ORDER BY permission.code
            """, nativeQuery = true)
    List<Permission> findActivePermissions(
            @Param("membershipId") Long membershipId,
            @Param("organizationId") Long organizationId);
}
