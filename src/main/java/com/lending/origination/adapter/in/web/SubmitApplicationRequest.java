package com.lending.origination.adapter.in.web;

import com.lending.shared.domain.Money;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record SubmitApplicationRequest(
        @NotNull(message = "customerId is required")
        UUID customerId,

        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "requestedPrincipal is required")
        Money requestedPrincipal,

        @Min(value = 1, message = "requestedTenureMonths must be >= 1")
        int requestedTenureMonths
) {
}
