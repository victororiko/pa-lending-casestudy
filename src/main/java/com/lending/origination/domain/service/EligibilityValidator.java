package com.lending.origination.domain.service;

import com.lending.customer.domain.model.Customer;
import com.lending.product.domain.model.LoanProduct;
import com.lending.shared.domain.Money;

public class EligibilityValidator {

    public record EligibilityResult(boolean eligible, String rejectionReason) {
        public static EligibilityResult eligible() {
            return new EligibilityResult(true, null);
        }

        public static EligibilityResult rejected(String reason) {
            return new EligibilityResult(false, reason);
        }
    }

    public static EligibilityResult validate(LoanProduct product, Customer customer,
                                              Money requestedPrincipal, int requestedTenureMonths,
                                              Money currentOutstandingPrincipal, int activeLoansCount) {
        // Rule 1: Product must be active and not deleted
        if (!product.isActive() || product.isDeleted()) {
            return EligibilityResult.rejected("Product is not active.");
        }

        // Rule 2: Requested principal within product bounds
        if (requestedPrincipal.isLessThan(product.getMinPrincipal())) {
            return EligibilityResult.rejected("Requested principal is below the minimum of " +
                    product.getMinPrincipal() + ".");
        }
        if (requestedPrincipal.isGreaterThan(product.getMaxPrincipal())) {
            return EligibilityResult.rejected("Requested principal exceeds the maximum of " +
                    product.getMaxPrincipal() + ".");
        }

        // Rule 3: Requested tenure within product bounds
        if (requestedTenureMonths < product.getMinTenureMonths()) {
            return EligibilityResult.rejected("Requested tenure is below the minimum of " +
                    product.getMinTenureMonths() + " months.");
        }
        if (requestedTenureMonths > product.getMaxTenureMonths()) {
            return EligibilityResult.rejected("Requested tenure exceeds the maximum of " +
                    product.getMaxTenureMonths() + " months.");
        }

        // Rule 4: Customer must not be deleted
        if (customer.isDeleted()) {
            return EligibilityResult.rejected("Customer not found.");
        }

        // Rule 5: Credit limit check (outstanding + requested <= creditLimit)
        Money totalExposure = currentOutstandingPrincipal.add(requestedPrincipal);
        if (totalExposure.isGreaterThan(customer.getCreditLimit())) {
            return EligibilityResult.rejected("Requested loan exceeds credit limit. Current outstanding: " +
                    currentOutstandingPrincipal + ", requested: " + requestedPrincipal +
                    ", credit limit: " + customer.getCreditLimit() + ".");
        }

        // Rule 6: Active loan count check
        if (activeLoansCount >= customer.getMaxActiveLoans()) {
            return EligibilityResult.rejected("Maximum active loans reached (" +
                    customer.getMaxActiveLoans() + ").");
        }

        return EligibilityResult.eligible();
    }
}
