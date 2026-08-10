package com.flakomencia.agendaflow.identity.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flakomencia.agendaflow.identity.domain.OrganizationMembership;
import com.flakomencia.agendaflow.identity.domain.MembershipStatus;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {

    @Query("""
            select membership from OrganizationMembership membership
            join fetch membership.organization
            join fetch membership.user
            where membership.user.id = :userId
              and membership.organization.id = :organizationId
            """)
    Optional<OrganizationMembership> findSessionMembership(
            @Param("userId") Long userId,
            @Param("organizationId") Long organizationId);

    @Query("""
            select membership from OrganizationMembership membership
            join fetch membership.organization
            join fetch membership.user
            where membership.id = :membershipId
            """)
    Optional<OrganizationMembership> findSessionMembershipById(@Param("membershipId") Long membershipId);

    boolean existsByOrganization_IdAndUser_IdAndStatus(
            Long organizationId, Long userId, MembershipStatus status);
}
