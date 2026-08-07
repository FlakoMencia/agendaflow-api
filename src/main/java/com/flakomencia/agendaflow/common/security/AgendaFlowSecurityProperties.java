package com.flakomencia.agendaflow.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("app.security")
public record AgendaFlowSecurityProperties(
        @Valid @NotNull Lockout lockout,
        @Valid @NotNull Token userToken,
        @Valid @NotNull Token serviceToken) {

    public record Lockout(@Min(1) int maximumFailedAttempts) { }

    public record Token(
            @NotBlank String secret,
            @NotBlank String issuer,
            @NotBlank String audience,
            @NotNull Duration timeToLive) { }
}
