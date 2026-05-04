package com.lending.servicing.domain.service;

import com.lending.product.domain.model.RepaymentFrequency;
import com.lending.servicing.domain.model.Installment;
import com.lending.servicing.domain.model.InstallmentStatus;
import com.lending.shared.domain.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class AmortisationCalculator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);
    private static final int SCALE = Money.SCALE;
    private static final RoundingMode ROUNDING = Money.ROUNDING;

    /**
     * Generate an amortisation schedule using the EMI (Equated Monthly Installment) formula.
     * For zero interest, splits principal equally.
     * The last installment absorbs any rounding remainder.
     */
    public static List<Installment> generateSchedule(UUID loanAccountId,
                                                      Money principal,
                                                      BigDecimal annualRate,
                                                      int tenureMonths,
                                                      RepaymentFrequency frequency,
                                                      LocalDate disbursementDate) {
        Currency currency = principal.getCurrency();
        int numberOfPeriods = calculateNumberOfPeriods(tenureMonths, frequency);
        BigDecimal periodicRate = calculatePeriodicRate(annualRate, frequency);

        BigDecimal emiAmount;
        BigDecimal totalInterest;

        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            // Zero interest: equal principal splits, no interest
            emiAmount = principal.getAmount().divide(BigDecimal.valueOf(numberOfPeriods), SCALE, ROUNDING);
            totalInterest = BigDecimal.ZERO;
        } else {
            // EMI = P * r * (1+r)^n / ((1+r)^n - 1)
            BigDecimal onePlusR = BigDecimal.ONE.add(periodicRate);
            BigDecimal onePlusRPowN = onePlusR.pow(numberOfPeriods, MC);
            BigDecimal numerator = principal.getAmount().multiply(periodicRate, MC).multiply(onePlusRPowN, MC);
            BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, MC);
            emiAmount = numerator.divide(denominator, SCALE, ROUNDING);
            totalInterest = emiAmount.multiply(BigDecimal.valueOf(numberOfPeriods), MC)
                    .subtract(principal.getAmount(), MC);
        }

        List<Installment> installments = new ArrayList<>();
        BigDecimal remainingPrincipal = principal.getAmount();
        BigDecimal totalPrincipalAllocated = BigDecimal.ZERO;
        BigDecimal totalInterestAllocated = BigDecimal.ZERO;

        for (int i = 1; i <= numberOfPeriods; i++) {
            LocalDate dueDate = calculateDueDate(disbursementDate, i, frequency);

            BigDecimal interestDue;
            BigDecimal principalDue;

            if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
                interestDue = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                principalDue = emiAmount;
            } else {
                interestDue = remainingPrincipal.multiply(periodicRate, MC).setScale(SCALE, ROUNDING);
                principalDue = emiAmount.subtract(interestDue).setScale(SCALE, ROUNDING);
            }

            // Last installment absorbs the rounding remainder
            if (i == numberOfPeriods) {
                principalDue = principal.getAmount().subtract(totalPrincipalAllocated).setScale(SCALE, ROUNDING);
                interestDue = (emiAmount.multiply(BigDecimal.valueOf(numberOfPeriods))
                        .subtract(principal.getAmount()))
                        .subtract(totalInterestAllocated)
                        .setScale(SCALE, ROUNDING);
                if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
                    interestDue = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                }
            }

            BigDecimal totalDue = principalDue.add(interestDue).setScale(SCALE, ROUNDING);

            Installment installment = new Installment();
            installment.setId(UUID.randomUUID());
            installment.setLoanAccountId(loanAccountId);
            installment.setInstallmentNumber(i);
            installment.setDueDate(dueDate);
            installment.setPrincipalDue(new Money(principalDue, currency));
            installment.setInterestDue(new Money(interestDue, currency));
            installment.setTotalDue(new Money(totalDue, currency));
            installment.setPrincipalPaid(Money.zero(currency));
            installment.setInterestPaid(Money.zero(currency));
            installment.setTotalPaid(Money.zero(currency));
            installment.setCurrency(currency.getCurrencyCode());
            installment.setStatus(InstallmentStatus.PENDING);

            installments.add(installment);

            totalPrincipalAllocated = totalPrincipalAllocated.add(principalDue);
            totalInterestAllocated = totalInterestAllocated.add(interestDue);
            remainingPrincipal = remainingPrincipal.subtract(principalDue);
        }

        return installments;
    }

    private static int calculateNumberOfPeriods(int tenureMonths, RepaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> tenureMonths;
            case BI_WEEKLY -> (int) Math.round(tenureMonths * 52.0 / 12.0 / 2.0);
            case WEEKLY -> (int) Math.round(tenureMonths * 52.0 / 12.0);
        };
    }

    private static BigDecimal calculatePeriodicRate(BigDecimal annualRate, RepaymentFrequency frequency) {
        BigDecimal rateDecimal = annualRate.divide(BigDecimal.valueOf(100), 10, ROUNDING);
        return switch (frequency) {
            case MONTHLY -> rateDecimal.divide(BigDecimal.valueOf(12), 10, ROUNDING);
            case BI_WEEKLY -> rateDecimal.divide(BigDecimal.valueOf(26), 10, ROUNDING);
            case WEEKLY -> rateDecimal.divide(BigDecimal.valueOf(52), 10, ROUNDING);
        };
    }

    private static LocalDate calculateDueDate(LocalDate disbursementDate, int periodNumber,
                                               RepaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> disbursementDate.plusMonths(periodNumber);
            case BI_WEEKLY -> disbursementDate.plusWeeks((long) periodNumber * 2);
            case WEEKLY -> disbursementDate.plusWeeks(periodNumber);
        };
    }
}
