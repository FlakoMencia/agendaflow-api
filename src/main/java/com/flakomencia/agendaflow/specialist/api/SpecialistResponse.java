package com.flakomencia.agendaflow.specialist.api;

import java.time.OffsetDateTime;

public record SpecialistResponse(
        Long id,
        Long organizationId,
        Long userId,
        String professionalName,
        String specialtyName,
        String biography,
        String licenseNumber,
        String photoUrl,
        String phone,
        String email,
        int simultaneousCapacity,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
