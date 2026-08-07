package com.flakomencia.agendaflow.identity.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flakomencia.agendaflow.identity.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @Query(value = """
            SELECT DISTINCT role.*
            FROM agendaflow.roles role
            JOIN agendaflow.membership_roles membership_role ON membership_role.role_id = role.id
            WHERE membership_role.membership_id = :membershipId
              AND role.is_active = TRUE
              AND (role.organization_id IS NULL OR role.organization_id = :organizationId)
            ORDER BY role.name
            """, nativeQuery = true)
    List<Role> findActiveRoles(
            @Param("membershipId") Long membershipId,
            @Param("organizationId") Long organizationId);
}
