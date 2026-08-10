package com.flakomencia.agendaflow.servicecatalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceCategoryUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        String description,
        Boolean active) {
}
