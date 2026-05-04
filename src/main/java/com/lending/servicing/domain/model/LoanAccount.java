package com.lending.servicing.domain.model;

import com.lending.product.domain.model.*;
import com.lending.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LoanAccount {

    private UUID id;
    private UUID applicationId;
    private UUID customerId;
    private UUID productId;
    private Money principal;
    private BigDecimal interestRatePerAnnum;
    private InterestAccrualMethod interestAccrualMethod;
    private int tenureMonths;
    private RepaymentFrequency repaymentFrequency;
    private int gracePeriodDays;
    private OriginationFeeChargeMethod originationFeeChargeMethod;
    private boolean allowEarlyRepayment;
    private OverpaymentPolicy overpaymentPolicy;
    private Money originationFee;
    private Money totalDisbursed;
    private Money outstandingBalance;
    private Money creditBalance;
    private LoanState state;
    private Instant disbursedAt;
    private Instant closedAt;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    private List<Installment> installments = new ArrayList<>();

    public LoanAccount() {
        this.id = UUID.randomUUID();
        this.state = LoanState.PENDING_DISBURSEMENT;
        this.createdBy = "system";
    }

    public void transitionTo(LoanState newState) {
        this.state = this.state.transitionTo(newState);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Money getPrincipal() { return principal; }
    public void setPrincipal(Money principal) { this.principal = principal; }

    public BigDecimal getInterestRatePerAnnum() { return interestRatePerAnnum; }
    public void setInterestRatePerAnnum(BigDecimal interestRatePerAnnum) { this.interestRatePerAnnum = interestRatePerAnnum; }

    public InterestAccrualMethod getInterestAccrualMethod() { return interestAccrualMethod; }
    public void setInterestAccrualMethod(InterestAccrualMethod interestAccrualMethod) { this.interestAccrualMethod = interestAccrualMethod; }

    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }

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

    public Money getOriginationFee() { return originationFee; }
    public void setOriginationFee(Money originationFee) { this.originationFee = originationFee; }

    public Money getTotalDisbursed() { return totalDisbursed; }
    public void setTotalDisbursed(Money totalDisbursed) { this.totalDisbursed = totalDisbursed; }

    public Money getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(Money outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public Money getCreditBalance() { return creditBalance; }
    public void setCreditBalance(Money creditBalance) { this.creditBalance = creditBalance; }

    public LoanState getState() { return state; }
    public void setState(LoanState state) { this.state = state; }

    public Instant getDisbursedAt() { return disbursedAt; }
    public void setDisbursedAt(Instant disbursedAt) { this.disbursedAt = disbursedAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<Installment> getInstallments() { return installments; }
    public void setInstallments(List<Installment> installments) { this.installments = installments; }
}
