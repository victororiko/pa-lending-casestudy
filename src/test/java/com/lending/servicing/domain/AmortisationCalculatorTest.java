package com.lending.servicing.domain;

import com.lending.product.domain.model.RepaymentFrequency;
import com.lending.servicing.domain.model.Installment;
import com.lending.servicing.domain.service.AmortisationCalculator;
import com.lending.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AmortisationCalculatorTest {

    private static final UUID LOAN_ID = UUID.randomUUID();

    @Test
    void standardEMI_12months() {
        Money principal = new Money("12000");
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, principal, new BigDecimal("12"), 12,
                RepaymentFrequency.MONTHLY, LocalDate.of(2026, 1, 15));

        assertEquals(12, schedule.size());

        // EMI should be approximately 1066.19
        BigDecimal firstEmi = schedule.get(0).getTotalDue().getAmount();
        assertTrue(firstEmi.compareTo(new BigDecimal("1060")) > 0);
        assertTrue(firstEmi.compareTo(new BigDecimal("1072")) < 0);

        // Sum of all installment principal should equal original principal exactly
        Money totalPrincipal = schedule.stream()
                .map(Installment::getPrincipalDue)
                .reduce(Money::add).orElseThrow();
        assertEquals(principal, totalPrincipal);
    }

    @Test
    void shortLoan_1month() {
        Money principal = new Money("1000");
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, principal, new BigDecimal("12"), 1,
                RepaymentFrequency.MONTHLY, LocalDate.of(2026, 1, 1));

        assertEquals(1, schedule.size());
        assertEquals(principal, schedule.get(0).getPrincipalDue());
        // Interest for 1 month at 12% APR = 1000 * 0.01 = 10
        assertTrue(schedule.get(0).getInterestDue().getAmount().compareTo(new BigDecimal("10")) == 0);
    }

    @Test
    void zeroInterest() {
        Money principal = new Money("6000");
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, principal, BigDecimal.ZERO, 6,
                RepaymentFrequency.MONTHLY, LocalDate.of(2026, 1, 1));

        assertEquals(6, schedule.size());

        // Each installment should have ~1000 principal, 0 interest
        for (Installment inst : schedule) {
            assertTrue(inst.getInterestDue().isZero());
        }

        // Total principal must equal original
        Money totalPrincipal = schedule.stream()
                .map(Installment::getPrincipalDue)
                .reduce(Money::add).orElseThrow();
        assertEquals(principal, totalPrincipal);
    }

    @Test
    void roundingRemainder_lastInstallment() {
        Money principal = new Money("10000");
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, principal, new BigDecimal("10"), 3,
                RepaymentFrequency.MONTHLY, LocalDate.of(2026, 1, 1));

        assertEquals(3, schedule.size());

        // Sum of all principal due must exactly equal the original principal
        Money totalPrincipal = schedule.stream()
                .map(Installment::getPrincipalDue)
                .reduce(Money::add).orElseThrow();
        assertEquals(principal, totalPrincipal);

        // Sum of total due should be principal + total interest
        Money totalDue = schedule.stream()
                .map(Installment::getTotalDue)
                .reduce(Money::add).orElseThrow();
        assertTrue(totalDue.isGreaterThan(principal));
    }

    @Test
    void monthEndRollover() {
        // Disbursement on Jan 31 — check Feb, Mar, Apr dates
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, new Money("6000"), new BigDecimal("12"), 6,
                RepaymentFrequency.MONTHLY, LocalDate.of(2026, 1, 31));

        assertEquals(6, schedule.size());
        assertEquals(LocalDate.of(2026, 2, 28), schedule.get(0).getDueDate());
        assertEquals(LocalDate.of(2026, 3, 31), schedule.get(1).getDueDate());
        assertEquals(LocalDate.of(2026, 4, 30), schedule.get(2).getDueDate());
        assertEquals(LocalDate.of(2026, 5, 31), schedule.get(3).getDueDate());
        assertEquals(LocalDate.of(2026, 6, 30), schedule.get(4).getDueDate());
        assertEquals(LocalDate.of(2026, 7, 31), schedule.get(5).getDueDate());
    }

    @Test
    void leapYear_feb29() {
        // 2028 is a leap year. Disbursement on Jan 31 → Feb due date should be Feb 29
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, new Money("12000"), new BigDecimal("12"), 12,
                RepaymentFrequency.MONTHLY, LocalDate.of(2028, 1, 31));

        assertEquals(12, schedule.size());
        assertEquals(LocalDate.of(2028, 2, 29), schedule.get(0).getDueDate());
    }

    @Test
    void weeklyFrequency() {
        Money principal = new Money("5200");
        // 12 months * 52/12 = 52 weeks
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, principal, new BigDecimal("10"), 12,
                RepaymentFrequency.WEEKLY, LocalDate.of(2026, 1, 1));

        assertEquals(52, schedule.size());

        Money totalPrincipal = schedule.stream()
                .map(Installment::getPrincipalDue)
                .reduce(Money::add).orElseThrow();
        assertEquals(principal, totalPrincipal);
    }

    @Test
    void biWeeklyFrequency() {
        Money principal = new Money("5200");
        // 12 months * 52/12/2 = 26 bi-weekly periods
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, principal, new BigDecimal("10"), 12,
                RepaymentFrequency.BI_WEEKLY, LocalDate.of(2026, 1, 1));

        assertEquals(26, schedule.size());

        Money totalPrincipal = schedule.stream()
                .map(Installment::getPrincipalDue)
                .reduce(Money::add).orElseThrow();
        assertEquals(principal, totalPrincipal);
    }

    @Test
    void installmentsAreNumbered_1indexed() {
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, new Money("1000"), new BigDecimal("12"), 3,
                RepaymentFrequency.MONTHLY, LocalDate.of(2026, 1, 1));

        assertEquals(1, schedule.get(0).getInstallmentNumber());
        assertEquals(2, schedule.get(1).getInstallmentNumber());
        assertEquals(3, schedule.get(2).getInstallmentNumber());
    }

    @Test
    void allInstallments_startAsPending() {
        List<Installment> schedule = AmortisationCalculator.generateSchedule(
                LOAN_ID, new Money("1000"), new BigDecimal("12"), 3,
                RepaymentFrequency.MONTHLY, LocalDate.of(2026, 1, 1));

        for (Installment inst : schedule) {
            assertEquals(com.lending.servicing.domain.model.InstallmentStatus.PENDING, inst.getStatus());
            assertTrue(inst.getPrincipalPaid().isZero());
            assertTrue(inst.getInterestPaid().isZero());
            assertTrue(inst.getTotalPaid().isZero());
        }
    }
}
