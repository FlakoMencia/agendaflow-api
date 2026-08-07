package com.flakomencia.agendaflow.identity.domain;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.organization.domain.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "organization_memberships", schema = "agendaflow")
public class OrganizationMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipStatus status;

    @Column(name = "joined_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "invited_at")
    private OffsetDateTime invitedAt;

    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected OrganizationMembership() {
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public AppUser getUser() { return user; }
    public MembershipStatus getStatus() { return status; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
    public OffsetDateTime getInvitedAt() { return invitedAt; }
    public Long getInvitedBy() { return invitedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
