package com.lending.servicing.adapter.in.web;

import com.lending.servicing.domain.model.LoanState;
import com.lending.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanAccountResponse(
        UUID id,
        UUID applicationId,
        UUID customerId,
        UUID productId,
        Money principal,
        BigDecimal interestRatePerAnnum,
        String interestAccrualMethod,
        int tenureMonths,
        String repaymentFrequency,
        int gracePeriodDays,
        String originationFeeChargeMethod,
        boolean allowEarlyRepayment,
        String overpaymentPolicy,
        Money originationFee,
        Money outstandingBalance,
        Money creditBalance,
        LoanState state,
        Instant disbursedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public static LoanAccountResponse from(com.lending.servicing.domain.model.LoanAccount la) {
        return new LoanAccountResponse(
                la.getId(), la.getApplicationId(), la.getCustomerId(), la.getProductId(),
                la.getPrincipal(), la.getInterestRatePerAnnum(),
                la.getInterestAccrualMethod().name(), la.getTenureMonths(),
                la.getRepaymentFrequency().name(), la.getGracePeriodDays(),
                la.getOriginationFeeChargeMethod().name(),
                la.isAllowEarlyRepayment(), la.getOverpaymentPolicy().name(),
                la.getOriginationFee(), la.getOutstandingBalance(), la.getCreditBalance(),
                la.getState(), la.getDisbursedAt(), la.getClosedAt(),
                la.getCreatedAt(), la.getUpdatedAt(), la.getVersion()
        );
    }
}
