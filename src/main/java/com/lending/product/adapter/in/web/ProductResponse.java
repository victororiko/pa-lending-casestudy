package com.lending.product.adapter.in.web;

import com.lending.product.domain.model.*;
import com.lending.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal interestRatePerAnnum,
        InterestAccrualMethod interestAccrualMethod,
        int minTenureMonths,
        int maxTenureMonths,
        Money minPrincipal,
        Money maxPrincipal,
        RepaymentFrequency repaymentFrequency,
        int gracePeriodDays,
        OriginationFeeChargeMethod originationFeeChargeMethod,
        boolean allowEarlyRepayment,
        OverpaymentPolicy overpaymentPolicy,
        List<FeeDefinitionResponse> feeSchedule,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public record FeeDefinitionResponse(
            FeeType feeType,
            FeeCalculationMethod calculationMethod,
            Money amount
    ) {
    }

    public static ProductResponse from(LoanProduct product) {
        List<FeeDefinitionResponse> fees = product.getFeeSchedule().stream()
                .map(f -> new FeeDefinitionResponse(f.getFeeType(), f.getCalculationMethod(), f.getAmount()))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getInterestRatePerAnnum(),
                product.getInterestAccrualMethod(),
                product.getMinTenureMonths(),
                product.getMaxTenureMonths(),
                product.getMinPrincipal(),
                product.getMaxPrincipal(),
                product.getRepaymentFrequency(),
                product.getGracePeriodDays(),
                product.getOriginationFeeChargeMethod(),
                product.isAllowEarlyRepayment(),
                product.getOverpaymentPolicy(),
                fees,
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getVersion()
        );
    }
}
