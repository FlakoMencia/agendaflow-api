package com.flakomencia.agendaflow.common.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AgendaFlowJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).forEach(authorities::add);
        }
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
