package com.lending.customer.adapter.in.web;

import com.lending.shared.domain.Money;
import jakarta.validation.constraints.*;

public record CreateCustomerRequest(
        @NotBlank(message = "firstName is required")
        @Size(max = 100, message = "firstName must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 100, message = "lastName must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "phoneNumber is required")
        String phoneNumber,

        @NotBlank(message = "nationalId is required")
        String nationalId,

        @NotNull(message = "creditLimit is required")
        Money creditLimit,

        @Min(value = 1, message = "maxActiveLoans must be >= 1")
        int maxActiveLoans
) {
}
