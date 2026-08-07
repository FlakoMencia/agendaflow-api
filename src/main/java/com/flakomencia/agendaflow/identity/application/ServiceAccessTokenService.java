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
public class ServiceAccessTokenService {
    public static final String NOTIFICATION_VALIDATE_GROUP = "notification:validate";

    private final JwtEncoder encoder;
    private final AgendaFlowSecurityProperties.Token settings;
    private final JwtIdentifierGenerator identifiers;
    private final Clock clock;

    public ServiceAccessTokenService(
            @Qualifier("serviceJwtEncoder") JwtEncoder encoder,
            AgendaFlowSecurityProperties properties,
            JwtIdentifierGenerator identifiers,
            Clock clock) {
        this.encoder = encoder;
        this.settings = properties.serviceToken();
        this.identifiers = identifiers;
        this.clock = clock;
    }

    public IssuedToken issueNotificationValidationToken() {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(settings.timeToLive());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .audience(List.of(settings.audience()))
                .subject("agendaflow-api")
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .id(identifiers.next())
                .claim("groups", List.of(NOTIFICATION_VALIDATE_GROUP))
                .claim("token_use", "service")
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new IssuedToken(value, settings.timeToLive().toSeconds());
    }
}
