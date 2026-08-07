package com.flakomencia.agendaflow.common.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AgendaFlowSecurityProperties.class)
public class SecurityTokenConfiguration implements EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Clock securityClock() {
        return Clock.systemUTC();
    }

    @Bean
    SecureRandom tokenSecureRandom() {
        return new SecureRandom();
    }

    @Bean("userJwtEncoder")
    JwtEncoder userJwtEncoder(AgendaFlowSecurityProperties properties) {
        if (properties.userToken().secret().equals(properties.serviceToken().secret())) {
            throw new IllegalStateException("JWT_SECRET and SERVICE_JWT_SECRET must be different");
        }
        return encoder(properties.userToken(), "JWT_SECRET");
    }

    @Bean("serviceJwtEncoder")
    JwtEncoder serviceJwtEncoder(AgendaFlowSecurityProperties properties) {
        return encoder(properties.serviceToken(), "SERVICE_JWT_SECRET");
    }

    @Bean("userJwtDecoder")
    @Primary
    JwtDecoder userJwtDecoder(AgendaFlowSecurityProperties properties) {
        return decoder(properties.userToken(), "user", "JWT_SECRET");
    }

    @Bean("serviceJwtDecoder")
    JwtDecoder serviceJwtDecoder(AgendaFlowSecurityProperties properties) {
        return decoder(properties.serviceToken(), "service", "SERVICE_JWT_SECRET");
    }

    private JwtEncoder encoder(AgendaFlowSecurityProperties.Token settings, String propertyName) {
        SecretKey key = secretKey(settings, propertyName);
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    private JwtDecoder decoder(
            AgendaFlowSecurityProperties.Token settings,
            String expectedTokenUse,
            String propertyName) {
        SecretKey key = secretKey(settings, propertyName);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> issuerAndTime = JwtValidators.createDefaultWithIssuer(settings.issuer());
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(settings.audience())
                ? OAuth2TokenValidatorResult.success()
                : invalid("JWT audience is not accepted");
        OAuth2TokenValidator<Jwt> tokenUse = jwt -> expectedTokenUse.equals(jwt.getClaimAsString("token_use"))
                ? OAuth2TokenValidatorResult.success()
                : invalid("JWT token_use is not accepted");
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerAndTime, audience, tokenUse));
        return decoder;
    }

    private SecretKey secretKey(AgendaFlowSecurityProperties.Token settings, String propertyName) {
        byte[] secret = settings.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(propertyName + " must contain at least 32 UTF-8 bytes for HS256");
        }
        if (!isDevelopmentProfile() && isPlaceholder(settings.secret())) {
            throw new IllegalStateException(propertyName + " must be provided securely outside local/test profiles");
        }
        if (settings.timeToLive().isZero() || settings.timeToLive().isNegative()) {
            throw new IllegalStateException(propertyName + " time-to-live must be positive");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    private boolean isDevelopmentProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("local")
                        || profile.equals("test")
                        || profile.equals("integration-test"));
    }

    private boolean isPlaceholder(String value) {
        String normalized = value.toLowerCase();
        return normalized.contains("change-me") || normalized.contains("local-only") || normalized.contains("test-only");
    }

    private OAuth2TokenValidatorResult invalid(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
