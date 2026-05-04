package com.lending.origination.domain;

import com.lending.customer.domain.model.Customer;
import com.lending.origination.domain.service.EligibilityValidator;
import com.lending.origination.domain.service.EligibilityValidator.EligibilityResult;
import com.lending.product.domain.model.*;
import com.lending.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EligibilityValidatorTest {

    private LoanProduct product;
    private Customer customer;

    @BeforeEach
    void setUp() {
        product = new LoanProduct();
        product.setName("Mkopo wa Majaribio");
        product.setInterestRatePerAnnum(new BigDecimal("14"));
        product.setMinTenureMonths(3);
        product.setMaxTenureMonths(12);
        product.setMinPrincipal(new Money("10000"));
        product.setMaxPrincipal(new Money("500000"));
        product.setRepaymentFrequency(RepaymentFrequency.MONTHLY);
        product.setActive(true);

        customer = new Customer();
        customer.setFirstName("Wanjiku");
        customer.setLastName("Kamau");
        customer.setEmail("wanjiku@example.co.ke");
        customer.setPhoneNumber("+254712345678");
        customer.setNationalId("12345678");
        customer.setCreditLimit(new Money("10000000"));
        customer.setMaxActiveLoans(3);
    }

    @Test
    void allConditionsMet_eligible() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("100000"), 6,
                Money.ZERO, 0);
        assertTrue(result.eligible());
    }

    @Test
    void principalBelowMinimum_rejected() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("5000"), 6,
                Money.ZERO, 0);
        assertFalse(result.eligible());
        assertTrue(result.rejectionReason().contains("below the minimum"));
    }

    @Test
    void principalAboveMaximum_rejected() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("600000"), 6,
                Money.ZERO, 0);
        assertFalse(result.eligible());
        assertTrue(result.rejectionReason().contains("exceeds the maximum"));
    }

    @Test
    void tenureBelowMinimum_rejected() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("100000"), 1,
                Money.ZERO, 0);
        assertFalse(result.eligible());
        assertTrue(result.rejectionReason().contains("below the minimum"));
    }

    @Test
    void tenureAboveMaximum_rejected() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("100000"), 24,
                Money.ZERO, 0);
        assertFalse(result.eligible());
        assertTrue(result.rejectionReason().contains("exceeds the maximum"));
    }

    @Test
    void productInactive_rejected() {
        product.setActive(false);
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("100000"), 6,
                Money.ZERO, 0);
        assertFalse(result.eligible());
        assertTrue(result.rejectionReason().contains("not active"));
    }

    @Test
    void customerDeleted_rejected() {
        customer.softDelete();
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("100000"), 6,
                Money.ZERO, 0);
        assertFalse(result.eligible());
        assertTrue(result.rejectionReason().contains("not found"));
    }

    @Test
    void exceedsCreditLimit_rejected() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("500000"), 6,
                new Money("9600000"), 0);
        assertFalse(result.eligible());
        assertTrue(result.rejectionReason().contains("credit limit"));
    }

    @Test
    void maxActiveLoansReached_rejected() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("100000"), 6,
                Money.ZERO, 3);
        assertFalse(result.eligible());
        assertTrue(result.rejectionReason().contains("Maximum active loans"));
    }

    @Test
    void creditLimitExactlyMet_eligible() {
        customer.setCreditLimit(new Money("500000"));
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("500000"), 6,
                Money.ZERO, 0);
        assertTrue(result.eligible());
    }

    @Test
    void principalExactlyAtMax_eligible() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("500000"), 6,
                Money.ZERO, 0);
        assertTrue(result.eligible());
    }

    @Test
    void principalExactlyAtMin_eligible() {
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("10000"), 6,
                Money.ZERO, 0);
        assertTrue(result.eligible());
    }

    @Test
    void customerWith1MaxActiveLoanAnd1Active_rejected() {
        customer.setMaxActiveLoans(1);
        EligibilityResult result = EligibilityValidator.validate(
                product, customer, new Money("100000"), 6,
                Money.ZERO, 1);
        assertFalse(result.eligible());
    }
}
