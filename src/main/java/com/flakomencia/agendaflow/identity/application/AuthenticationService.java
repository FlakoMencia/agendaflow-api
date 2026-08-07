package com.flakomencia.agendaflow.identity.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.common.security.AgendaFlowSecurityProperties;
import com.flakomencia.agendaflow.identity.api.ActiveOrganizationResponse;
import com.flakomencia.agendaflow.identity.api.AuthenticatedUserResponse;
import com.flakomencia.agendaflow.identity.api.CurrentSessionResponse;
import com.flakomencia.agendaflow.identity.api.LoginRequest;
import com.flakomencia.agendaflow.identity.api.LoginResponse;
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

@Service
public class AuthenticationService {
    private final AppUserRepository users;
    private final OrganizationMembershipRepository memberships;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final PasswordEncoder passwordEncoder;
    private final UserAccessTokenService tokenService;
    private final int maximumFailedAttempts;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthenticationService(
            AppUserRepository users,
            OrganizationMembershipRepository memberships,
            RoleRepository roles,
            PermissionRepository permissions,
            PasswordEncoder passwordEncoder,
            UserAccessTokenService tokenService,
            AgendaFlowSecurityProperties properties,
            Clock clock,
            SecureRandom secureRandom) {
        this.users = users;
        this.memberships = memberships;
        this.roles = roles;
        this.permissions = permissions;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.maximumFailedAttempts = properties.lockout().maximumFailedAttempts();
        this.clock = clock;
        byte[] randomPassword = new byte[32];
        secureRandom.nextBytes(randomPassword);
        this.dummyPasswordHash = passwordEncoder.encode(HexFormat.of().formatHex(randomPassword));
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AppUser user = users.findForAuthentication(normalizedEmail).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new InvalidCredentialsException();
        }

        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!user.isActive() || user.isLocked() || user.getDeletedAt() != null) {
            throw new InvalidCredentialsException();
        }
        if (!passwordMatches) {
            user.recordFailedLogin(maximumFailedAttempts);
            users.save(user);
            throw new InvalidCredentialsException();
        }

        OrganizationMembership membership = memberships
                .findSessionMembership(user.getId(), request.organizationId())
                .filter(this::isActiveSession)
                .orElseThrow(InvalidCredentialsException::new);
        SessionParts session = loadSessionParts(membership);
        user.recordSuccessfulLogin(OffsetDateTime.now(clock));
        IssuedToken issuedToken = tokenService.issue(session.identity());
        return new LoginResponse(
                issuedToken.value(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                membership.getId(),
                session.user(),
                session.organization(),
                session.identity().roles(),
                session.identity().permissions());
    }

    @Transactional(readOnly = true)
    public CurrentSessionResponse currentSession(Jwt jwt) {
        Long userId = longClaim(jwt, "user_id");
        Long membershipId = longClaim(jwt, "membership_id");
        Long organizationId = longClaim(jwt, "organization_id");
        OrganizationMembership membership = memberships.findSessionMembershipById(membershipId)
                .filter(value -> value.getUser().getId().equals(userId))
                .filter(value -> value.getOrganization().getId().equals(organizationId))
                .filter(this::isActiveSession)
                .orElseThrow(InvalidSessionException::new);
        SessionParts session = loadSessionParts(membership);
        return new CurrentSessionResponse(
                membershipId,
                session.user(),
                session.organization(),
                session.identity().roles(),
                session.identity().permissions());
    }

    private SessionParts loadSessionParts(OrganizationMembership membership) {
        Long organizationId = membership.getOrganization().getId();
        List<String> roleNames = roles.findActiveRoles(membership.getId(), organizationId).stream()
                .map(Role::getName)
                .distinct()
                .sorted()
                .toList();
        List<String> permissionCodes = permissions.findActivePermissions(membership.getId(), organizationId).stream()
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                membership.getUser().getId(), membership.getId(), organizationId, roleNames, permissionCodes);
        return new SessionParts(identity, userResponse(membership.getUser()), organizationResponse(membership.getOrganization()));
    }

    private boolean isActiveSession(OrganizationMembership membership) {
        return membership.getStatus() == MembershipStatus.ACTIVE
                && membership.getUser().isActive()
                && !membership.getUser().isLocked()
                && membership.getUser().getDeletedAt() == null
                && membership.getOrganization().getDeletedAt() == null
                && membership.getOrganization().getStatus() == OrganizationStatus.ACTIVE;
    }

    private AuthenticatedUserResponse userResponse(AppUser user) {
        return new AuthenticatedUserResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getMiddleName(),
                user.getLastName(), user.getSecondLastName(), user.getPreferredLanguage());
    }

    private ActiveOrganizationResponse organizationResponse(Organization organization) {
        return new ActiveOrganizationResponse(
                organization.getId(), organization.getLegalName(), organization.getTradeName(),
                organization.getTimezone(), organization.getLanguageCode(), organization.getStatus());
    }

    private Long longClaim(Jwt jwt, String name) {
        Object claim = jwt.getClaim(name);
        if (!(claim instanceof Number number)) {
            throw new InvalidSessionException();
        }
        return number.longValue();
    }

    private record SessionParts(
            AuthenticatedIdentity identity,
            AuthenticatedUserResponse user,
            ActiveOrganizationResponse organization) { }
}
