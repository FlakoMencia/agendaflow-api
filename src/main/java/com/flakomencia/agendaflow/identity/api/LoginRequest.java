package com.flakomencia.agendaflow.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "admin@example.com")
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(format = "password")
        @NotBlank @Size(max = 200) String password,
        @Schema(example = "1")
        @NotNull @Positive Long organizationId) {
}
