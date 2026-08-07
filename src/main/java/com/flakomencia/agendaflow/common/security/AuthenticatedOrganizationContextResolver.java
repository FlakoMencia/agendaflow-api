package com.flakomencia.agendaflow.common.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.flakomencia.agendaflow.identity.application.InvalidSessionException;

@Component
public class AuthenticatedOrganizationContextResolver {

    public AuthenticatedOrganizationContext current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("An authenticated JWT is required");
        }
        return new AuthenticatedOrganizationContext(
                longClaim(jwt, "user_id"),
                longClaim(jwt, "membership_id"),
                longClaim(jwt, "organization_id"),
                stringSet(jwt.getClaimAsStringList("roles")),
                stringSet(jwt.getClaimAsStringList("permissions")));
    }

    private Long longClaim(Jwt jwt, String name) {
        Object claim = jwt.getClaim(name);
        if (!(claim instanceof Number number)) {
            throw new InvalidSessionException();
        }
        return number.longValue();
    }

    private Set<String> stringSet(List<String> values) {
        return values == null ? Set.of() : new HashSet<>(values);
    }
}
