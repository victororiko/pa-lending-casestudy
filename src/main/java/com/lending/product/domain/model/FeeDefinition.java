package com.lending.product.domain.model;

import com.lending.shared.domain.Money;

import java.util.Objects;
import java.util.UUID;

public class FeeDefinition {

    private UUID id;
    private FeeType feeType;
    private FeeCalculationMethod calculationMethod;
    private Money amount;

    public FeeDefinition(UUID id, FeeType feeType, FeeCalculationMethod calculationMethod, Money amount) {
        this.id = id;
        this.feeType = Objects.requireNonNull(feeType);
        this.calculationMethod = Objects.requireNonNull(calculationMethod);
        this.amount = Objects.requireNonNull(amount);
        validate();
    }

    private void validate() {
        // PERCENTAGE_OF_OUTSTANDING is valid only for PREPAYMENT_PENALTY
        if (calculationMethod == FeeCalculationMethod.PERCENTAGE_OF_OUTSTANDING
                && feeType != FeeType.PREPAYMENT_PENALTY) {
            throw new IllegalArgumentException(
                    "PERCENTAGE_OF_OUTSTANDING calculation method is only valid for PREPAYMENT_PENALTY fee type.");
        }

        // PREPAYMENT_PENALTY may only use FLAT or PERCENTAGE_OF_OUTSTANDING
        if (feeType == FeeType.PREPAYMENT_PENALTY
                && calculationMethod == FeeCalculationMethod.PERCENTAGE_OF_PRINCIPAL) {
            throw new IllegalArgumentException(
                    "PREPAYMENT_PENALTY fee may only use FLAT or PERCENTAGE_OF_OUTSTANDING calculation methods.");
        }
    }

    public UUID getId() {
        return id;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public FeeCalculationMethod getCalculationMethod() {
        return calculationMethod;
    }

    public Money getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeeDefinition that = (FeeDefinition) o;
        return feeType == that.feeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeType);
    }
}
