package com.flakomencia.agendaflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.flakomencia.agendaflow.common.security.AgendaFlowSecurityProperties;
import com.flakomencia.agendaflow.identity.api.LoginRequest;
import com.flakomencia.agendaflow.identity.domain.AppUser;
import com.flakomencia.agendaflow.identity.domain.MembershipStatus;
import com.flakomencia.agendaflow.identity.domain.OrganizationMembership;
import com.flakomencia.agendaflow.identity.domain.Permission;
import com.flakomencia.agendaflow.identity.domain.Role;
import com.flakomencia.agendaflow.identity.infrastructure.AppUserRepository;
import com.flakomencia.agendaflow.identity.infrastructure.OrganizationMembershipRepository;
import com.flakomencia.agendaflow.identity.infrastructure.PermissionRepository;
import com.flakomencia.agendaflow.identity.infrastructure.RoleRepository;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;

class AuthenticationServiceTest {
    private AppUserRepository users;
    private OrganizationMembershipRepository memberships;
    private RoleRepository roles;
    private PermissionRepository permissions;
    private PasswordEncoder passwordEncoder;
    private UserAccessTokenService tokens;
    private AuthenticationService service;
    private AppUser user;
    private OrganizationMembership membership;
    private Organization organization;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        memberships = mock(OrganizationMembershipRepository.class);
        roles = mock(RoleRepository.class);
        permissions = mock(PermissionRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokens = mock(UserAccessTokenService.class);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("dummy-hash");
        service = new AuthenticationService(
                users, memberships, roles, permissions, passwordEncoder, tokens,
                properties(), Clock.fixed(Instant.parse("2026-08-03T12:00:00Z"), ZoneOffset.UTC), new SecureRandom());
        user = mock(AppUser.class);
        membership = mock(OrganizationMembership.class);
        organization = mock(Organization.class);
        when(user.getId()).thenReturn(10L);
        when(user.getEmail()).thenReturn("admin@example.com");
        when(user.getPasswordHash()).thenReturn("stored-hash");
        when(user.isActive()).thenReturn(true);
        when(membership.getId()).thenReturn(20L);
        when(membership.getUser()).thenReturn(user);
        when(membership.getOrganization()).thenReturn(organization);
        when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(organization.getId()).thenReturn(30L);
        when(organization.getStatus()).thenReturn(OrganizationStatus.ACTIVE);
    }

    @Test
    void authenticatesAnActiveMembershipAndResetsFailedAttempts() {
        Role role = mock(Role.class);
        Permission permission = mock(Permission.class);
        when(role.getName()).thenReturn("ORGANIZATION_ADMIN");
        when(permission.getCode()).thenReturn("ORGANIZATION_VIEW");
        when(users.findForAuthentication("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "stored-hash")).thenReturn(true);
        when(memberships.findSessionMembership(10L, 30L)).thenReturn(Optional.of(membership));
        when(roles.findActiveRoles(20L, 30L)).thenReturn(List.of(role));
        when(permissions.findActivePermissions(20L, 30L)).thenReturn(List.of(permission));
        when(tokens.issue(org.mockito.ArgumentMatchers.any())).thenReturn(new IssuedToken("jwt", 1800));

        var response = service.login(new LoginRequest(" ADMIN@example.com ", "correct", 30L));

        assertThat(response.accessToken()).isEqualTo("jwt");
        assertThat(response.roles()).containsExactly("ORGANIZATION_ADMIN");
        assertThat(response.permissions()).containsExactly("ORGANIZATION_VIEW");
        verify(user).recordSuccessfulLogin(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsTheSameGenericErrorForUnknownUserAndWrongPassword() {
        when(users.findForAuthentication("missing@example.com")).thenReturn(Optional.empty());
        assertInvalid(new LoginRequest("missing@example.com", "wrong", 30L));

        when(users.findForAuthentication("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);
        assertInvalid(new LoginRequest("admin@example.com", "wrong", 30L));
        verify(user).recordFailedLogin(5);
        verify(users).save(user);
    }

    @Test
    void rejectsInactiveOrLockedUsersWithoutRevealingTheirState() {
        when(users.findForAuthentication("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "stored-hash")).thenReturn(true);
        when(user.isActive()).thenReturn(false);
        assertInvalid(new LoginRequest("admin@example.com", "correct", 30L));

        when(user.isActive()).thenReturn(true);
        when(user.isLocked()).thenReturn(true);
        assertInvalid(new LoginRequest("admin@example.com", "correct", 30L));
    }

    @Test
    void rejectsMissingOrInactiveMembershipAndSuspendedOrganizationGenerically() {
        when(users.findForAuthentication("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "stored-hash")).thenReturn(true);
        when(memberships.findSessionMembership(10L, 30L)).thenReturn(Optional.empty());
        assertInvalid(new LoginRequest("admin@example.com", "correct", 30L));

        when(memberships.findSessionMembership(10L, 30L)).thenReturn(Optional.of(membership));
        when(membership.getStatus()).thenReturn(MembershipStatus.SUSPENDED);
        assertInvalid(new LoginRequest("admin@example.com", "correct", 30L));

        when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(organization.getStatus()).thenReturn(OrganizationStatus.SUSPENDED);
        assertInvalid(new LoginRequest("admin@example.com", "correct", 30L));
    }

    private void assertInvalid(LoginRequest request) {
        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email, password or organization is invalid");
    }

    private AgendaFlowSecurityProperties properties() {
        var userToken = new AgendaFlowSecurityProperties.Token("x".repeat(32), "issuer", "web", Duration.ofMinutes(30));
        var serviceToken = new AgendaFlowSecurityProperties.Token("y".repeat(32), "issuer", "service", Duration.ofMinutes(5));
        return new AgendaFlowSecurityProperties(new AgendaFlowSecurityProperties.Lockout(5), userToken, serviceToken);
    }
}
