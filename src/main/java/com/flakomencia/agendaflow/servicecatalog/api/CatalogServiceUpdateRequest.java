package com.flakomencia.agendaflow.servicecatalog.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CatalogServiceUpdateRequest(
        @Positive Long categoryId,
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull @Positive Integer durationMinutes,
        @PositiveOrZero Integer preparationMinutes,
        @PositiveOrZero Integer cleanupMinutes,
        @Digits(integer = 10, fraction = 2) @DecimalMin("0.00") BigDecimal price,
        @Size(min = 3, max = 3) String currencyCode,
        Boolean requiresApproval,
        Boolean allowsOnlineBooking,
        Boolean active,
        @Size(max = 20) String colorCode) {
}
