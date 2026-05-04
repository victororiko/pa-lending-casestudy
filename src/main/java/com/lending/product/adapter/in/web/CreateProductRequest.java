package com.lending.product.adapter.in.web;

import com.lending.product.domain.model.*;
import com.lending.shared.domain.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,

        @NotNull(message = "interestRatePerAnnum is required")
        @DecimalMin(value = "0", message = "interestRatePerAnnum must be >= 0")
        @DecimalMax(value = "100", message = "interestRatePerAnnum must be <= 100")
        BigDecimal interestRatePerAnnum,

        InterestAccrualMethod interestAccrualMethod,

        @Min(value = 1, message = "minTenureMonths must be >= 1")
        int minTenureMonths,

        @Min(value = 1, message = "maxTenureMonths must be >= 1")
        int maxTenureMonths,

        @NotNull(message = "minPrincipal is required")
        Money minPrincipal,

        @NotNull(message = "maxPrincipal is required")
        Money maxPrincipal,

        @NotNull(message = "repaymentFrequency is required")
        RepaymentFrequency repaymentFrequency,

        @Min(value = 0, message = "gracePeriodDays must be >= 0")
        int gracePeriodDays,

        OriginationFeeChargeMethod originationFeeChargeMethod,

        boolean allowEarlyRepayment,

        OverpaymentPolicy overpaymentPolicy,

        @Valid
        List<FeeDefinitionRequest> feeSchedule
) {
    public record FeeDefinitionRequest(
            @NotNull(message = "feeType is required")
            FeeType feeType,

            @NotNull(message = "calculationMethod is required")
            FeeCalculationMethod calculationMethod,

            @NotNull(message = "amount is required")
            Money amount
    ) {
    }
}
