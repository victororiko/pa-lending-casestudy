package com.lending.servicing.adapter.in.web;

import com.lending.shared.domain.Money;
import jakarta.validation.constraints.NotNull;

public record RecordRepaymentRequest(
        @NotNull(message = "amount is required")
        Money amount
) {
}
