package com.lending.product.domain.model;

import com.lending.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LoanProduct {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal interestRatePerAnnum;
    private InterestAccrualMethod interestAccrualMethod;
    private int minTenureMonths;
    private int maxTenureMonths;
    private Money minPrincipal;
    private Money maxPrincipal;
    private RepaymentFrequency repaymentFrequency;
    private int gracePeriodDays;
    private OriginationFeeChargeMethod originationFeeChargeMethod;
    private boolean allowEarlyRepayment;
    private OverpaymentPolicy overpaymentPolicy;
    private List<FeeDefinition> feeSchedule;
    private boolean active;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public LoanProduct() {
        this.id = UUID.randomUUID();
        this.feeSchedule = new ArrayList<>();
        this.active = true;
        this.interestAccrualMethod = InterestAccrualMethod.PRE_COMPUTED;
        this.gracePeriodDays = 0;
        this.originationFeeChargeMethod = OriginationFeeChargeMethod.DEDUCTED_FROM_DISBURSEMENT;
        this.allowEarlyRepayment = true;
        this.overpaymentPolicy = OverpaymentPolicy.REJECT;
    }

    public void validate() {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (name.length() > 100) throw new IllegalArgumentException("name must not exceed 100 characters");

        Objects.requireNonNull(interestRatePerAnnum, "interestRatePerAnnum must not be null");
        if (interestRatePerAnnum.compareTo(BigDecimal.ZERO) <= 0 && interestRatePerAnnum.compareTo(BigDecimal.ZERO) != 0) {
            // Allow 0% interest
        }
        if (interestRatePerAnnum.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("interestRatePerAnnum must be >= 0");
        }
        if (interestRatePerAnnum.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("interestRatePerAnnum must be <= 100");
        }

        if (minTenureMonths < 1) throw new IllegalArgumentException("minTenureMonths must be >= 1");
        if (maxTenureMonths < minTenureMonths) {
            throw new IllegalArgumentException("maxTenureMonths must be >= minTenureMonths");
        }

        Objects.requireNonNull(minPrincipal, "minPrincipal must not be null");
        Objects.requireNonNull(maxPrincipal, "maxPrincipal must not be null");
        if (!minPrincipal.isPositive()) throw new IllegalArgumentException("minPrincipal must be > 0");
        if (maxPrincipal.isLessThan(minPrincipal)) {
            throw new IllegalArgumentException("maxPrincipal must be >= minPrincipal");
        }

        if (gracePeriodDays < 0) throw new IllegalArgumentException("gracePeriodDays must be >= 0");

        // Invariant: If allowEarlyRepayment is false, fee schedule must not contain PREPAYMENT_PENALTY
        if (!allowEarlyRepayment && feeSchedule != null) {
            boolean hasPrepaymentPenalty = feeSchedule.stream()
                    .anyMatch(f -> f.getFeeType() == FeeType.PREPAYMENT_PENALTY);
            if (hasPrepaymentPenalty) {
                throw new IllegalArgumentException(
                        "Cannot have PREPAYMENT_PENALTY fee when allowEarlyRepayment is false");
            }
        }

        // Invariant: At most one fee of each type
        if (feeSchedule != null) {
            long distinctTypes = feeSchedule.stream().map(FeeDefinition::getFeeType).distinct().count();
            if (distinctTypes < feeSchedule.size()) {
                throw new IllegalArgumentException("A product must have at most one fee of each FeeType");
            }
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.active = false;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getInterestRatePerAnnum() { return interestRatePerAnnum; }
    public void setInterestRatePerAnnum(BigDecimal interestRatePerAnnum) { this.interestRatePerAnnum = interestRatePerAnnum; }

    public InterestAccrualMethod getInterestAccrualMethod() { return interestAccrualMethod; }
    public void setInterestAccrualMethod(InterestAccrualMethod interestAccrualMethod) { this.interestAccrualMethod = interestAccrualMethod; }

    public int getMinTenureMonths() { return minTenureMonths; }
    public void setMinTenureMonths(int minTenureMonths) { this.minTenureMonths = minTenureMonths; }

    public int getMaxTenureMonths() { return maxTenureMonths; }
    public void setMaxTenureMonths(int maxTenureMonths) { this.maxTenureMonths = maxTenureMonths; }

    public Money getMinPrincipal() { return minPrincipal; }
    public void setMinPrincipal(Money minPrincipal) { this.minPrincipal = minPrincipal; }

    public Money getMaxPrincipal() { return maxPrincipal; }
    public void setMaxPrincipal(Money maxPrincipal) { this.maxPrincipal = maxPrincipal; }

    public RepaymentFrequency getRepaymentFrequency() { return repaymentFrequency; }
    public void setRepaymentFrequency(RepaymentFrequency repaymentFrequency) { this.repaymentFrequency = repaymentFrequency; }

    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }

    public OriginationFeeChargeMethod getOriginationFeeChargeMethod() { return originationFeeChargeMethod; }
    public void setOriginationFeeChargeMethod(OriginationFeeChargeMethod originationFeeChargeMethod) { this.originationFeeChargeMethod = originationFeeChargeMethod; }

    public boolean isAllowEarlyRepayment() { return allowEarlyRepayment; }
    public void setAllowEarlyRepayment(boolean allowEarlyRepayment) { this.allowEarlyRepayment = allowEarlyRepayment; }

    public OverpaymentPolicy getOverpaymentPolicy() { return overpaymentPolicy; }
    public void setOverpaymentPolicy(OverpaymentPolicy overpaymentPolicy) { this.overpaymentPolicy = overpaymentPolicy; }

    public List<FeeDefinition> getFeeSchedule() { return feeSchedule; }
    public void setFeeSchedule(List<FeeDefinition> feeSchedule) { this.feeSchedule = feeSchedule; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
