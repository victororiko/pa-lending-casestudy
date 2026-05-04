package com.lending.servicing.domain;

import com.lending.product.domain.model.OverpaymentPolicy;
import com.lending.servicing.domain.model.Installment;
import com.lending.servicing.domain.model.InstallmentStatus;
import com.lending.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepaymentAllocationServiceTest {

    private static final Currency KES = Currency.getInstance("KES");

    private Installment createInstallment(int number, String principalDue, String interestDue) {
        Installment i = new Installment();
        i.setId(UUID.randomUUID());
        i.setLoanAccountId(UUID.randomUUID());
        i.setInstallmentNumber(number);
        i.setDueDate(LocalDate.now().plusMonths(number));
        i.setPrincipalDue(new Money(principalDue, KES));
        i.setInterestDue(new Money(interestDue, KES));
        i.setTotalDue(new Money(principalDue, KES).add(new Money(interestDue, KES)));
        i.setPrincipalPaid(Money.zero(KES));
        i.setInterestPaid(Money.zero(KES));
        i.setTotalPaid(Money.zero(KES));
        i.setCurrency("KES");
        i.setStatus(InstallmentStatus.PENDING);
        return i;
    }

    @Test
    void fullSingleInstallment_interestFirst() {
        Installment installment = createInstallment(1, "16320.7000", "1041.7000");
        Money payment = new Money("17362.4000", KES);

        // Pay interest first
        Money interestRemaining = installment.remainingInterest();
        Money principalRemaining = installment.remainingPrincipal();

        Money interestPayment = payment.isGreaterThanOrEqual(interestRemaining) ? interestRemaining : payment;
        Money remaining = payment.subtract(interestPayment);
        Money principalPayment = remaining.isGreaterThanOrEqual(principalRemaining) ? principalRemaining : remaining;

        installment.applyPayment(interestPayment, principalPayment);

        assertEquals(InstallmentStatus.PAID, installment.getStatus());
        assertTrue(installment.isFullyPaid());
        assertEquals(new Money("1041.7000", KES), installment.getInterestPaid());
        assertEquals(new Money("16320.7000", KES), installment.getPrincipalPaid());
    }

    @Test
    void partialPayment_interestBeforePrincipal() {
        Installment installment = createInstallment(1, "16320.7000", "1041.7000");
        Money payment = new Money("5000.0000", KES);

        Money interestRemaining = installment.remainingInterest();
        Money interestPayment = payment.isGreaterThanOrEqual(interestRemaining) ? interestRemaining : payment;
        Money remaining = payment.subtract(interestPayment);
        Money principalPayment = remaining;

        installment.applyPayment(interestPayment, principalPayment);

        assertEquals(InstallmentStatus.PARTIALLY_PAID, installment.getStatus());
        assertFalse(installment.isFullyPaid());
        // Interest fully paid: 1041.70
        assertEquals(new Money("1041.7000", KES), installment.getInterestPaid());
        // Remaining to principal: 5000 - 1041.70 = 3958.30
        assertEquals(new Money("3958.3000", KES), installment.getPrincipalPaid());
    }

    @Test
    void zeroAmount_rejected() {
        Money zero = Money.zero(KES);
        assertFalse(zero.isPositive());
    }

    @Test
    void paymentToPartiallyPaidInstallment() {
        Installment installment = createInstallment(1, "16320.7000", "1041.7000");
        // First partial payment
        installment.applyPayment(new Money("1041.7000", KES), new Money("3958.3000", KES));
        assertEquals(InstallmentStatus.PARTIALLY_PAID, installment.getStatus());

        // Second payment covers the rest
        Money remaining = installment.remainingPrincipal();
        assertEquals(new Money("12362.4000", KES), remaining);

        installment.applyPayment(Money.zero(KES), remaining);
        assertEquals(InstallmentStatus.PAID, installment.getStatus());
    }

    @Test
    void overpaymentSpanning2Installments() {
        Installment inst1 = createInstallment(1, "8000.0000", "1000.0000");
        Installment inst2 = createInstallment(2, "8000.0000", "800.0000");
        Money payment = new Money("15000.0000", KES);
        Money remaining = payment;

        // Allocate to installment 1
        Money interest1 = inst1.remainingInterest();
        Money principal1 = inst1.remainingPrincipal();
        Money intPay1 = remaining.isGreaterThanOrEqual(interest1) ? interest1 : remaining;
        remaining = remaining.subtract(intPay1);
        Money prinPay1 = remaining.isGreaterThanOrEqual(principal1) ? principal1 : remaining;
        remaining = remaining.subtract(prinPay1);
        inst1.applyPayment(intPay1, prinPay1);

        assertEquals(InstallmentStatus.PAID, inst1.getStatus());
        // Remaining: 15000 - 1000 - 8000 = 6000
        assertEquals(new Money("6000.0000", KES), remaining);

        // Allocate remainder to installment 2
        Money interest2 = inst2.remainingInterest();
        Money intPay2 = remaining.isGreaterThanOrEqual(interest2) ? interest2 : remaining;
        remaining = remaining.subtract(intPay2);
        Money principal2 = inst2.remainingPrincipal();
        Money prinPay2 = remaining.isGreaterThanOrEqual(principal2) ? principal2 : remaining;
        remaining = remaining.subtract(prinPay2);
        inst2.applyPayment(intPay2, prinPay2);

        // Installment 2 should be partially paid: 6000 - 800 interest = 5200 to principal
        assertEquals(InstallmentStatus.PARTIALLY_PAID, inst2.getStatus());
        assertEquals(new Money("800.0000", KES), inst2.getInterestPaid());
        assertEquals(new Money("5200.0000", KES), inst2.getPrincipalPaid());
    }
}
