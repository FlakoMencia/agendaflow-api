package com.flakomencia.agendaflow.branch.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchUpdateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 40) String code,
        @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @Size(max = 150) String addressLine1,
        @Size(max = 150) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 10) String stateCode,
        @Size(max = 15) String postalCode,
        @Size(min = 2, max = 2) String countryCode,
        @Size(max = 60) String timezone,
        @Digits(integer = 3, fraction = 6) @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @Digits(integer = 3, fraction = 6) @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        Boolean active) {
}
