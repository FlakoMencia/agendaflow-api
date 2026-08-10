package com.flakomencia.agendaflow.specialist.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SpecialistUpdateRequest(
        @Positive Long userId,
        @NotBlank @Size(max = 160) String professionalName,
        @Size(max = 150) String specialtyName,
        String biography,
        @Size(max = 80) String licenseNumber,
        @Size(max = 500) String photoUrl,
        @Size(max = 30) String phone,
        @Email @Size(max = 254) String email,
        @Positive Integer simultaneousCapacity,
        Boolean active) {
}
