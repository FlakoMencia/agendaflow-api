package com.flakomencia.agendaflow.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.flakomencia.agendaflow.common.security.AgendaFlowSecurityProperties;

@Service
public class UserAccessTokenService {
    private final JwtEncoder encoder;
    private final AgendaFlowSecurityProperties.Token settings;
    private final JwtIdentifierGenerator identifiers;
    private final Clock clock;

    public UserAccessTokenService(
            @Qualifier("userJwtEncoder") JwtEncoder encoder,
            AgendaFlowSecurityProperties properties,
            JwtIdentifierGenerator identifiers,
            Clock clock) {
        this.encoder = encoder;
        this.settings = properties.userToken();
        this.identifiers = identifiers;
        this.clock = clock;
    }

    public IssuedToken issue(AuthenticatedIdentity identity) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(settings.timeToLive());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .audience(List.of(settings.audience()))
                .subject(identity.userId().toString())
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .id(identifiers.next())
                .claim("user_id", identity.userId())
                .claim("membership_id", identity.membershipId())
                .claim("organization_id", identity.organizationId())
                .claim("roles", identity.roles())
                .claim("permissions", identity.permissions())
                .claim("token_use", "user")
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new IssuedToken(value, settings.timeToLive().toSeconds());
    }
}
