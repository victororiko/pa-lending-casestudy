package com.lending.origination.domain.model;

import com.lending.product.domain.model.FeeDefinition;
import com.lending.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class LoanApplication {

    private UUID id;
    private UUID customerId;
    private UUID productId;
    private Money requestedPrincipal;
    private int requestedTenureMonths;
    private ApplicationStatus status;
    private String rejectionReason;
    private Instant approvedAt;
    private BigDecimal snapshotInterestRate;
    private List<FeeDefinition> snapshotFeeSchedule;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public LoanApplication() {
        this.id = UUID.randomUUID();
        this.status = ApplicationStatus.PENDING;
    }

    public void approve(BigDecimal snapshotInterestRate, List<FeeDefinition> snapshotFeeSchedule) {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING applications can be approved. Current status: " + this.status);
        }
        this.status = ApplicationStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.snapshotInterestRate = snapshotInterestRate;
        this.snapshotFeeSchedule = snapshotFeeSchedule;
    }

    public void reject(String reason) {
        if (this.status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING applications can be rejected. Current status: " + this.status);
        }
        this.status = ApplicationStatus.REJECTED;
        this.rejectionReason = reason;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Money getRequestedPrincipal() { return requestedPrincipal; }
    public void setRequestedPrincipal(Money requestedPrincipal) { this.requestedPrincipal = requestedPrincipal; }

    public int getRequestedTenureMonths() { return requestedTenureMonths; }
    public void setRequestedTenureMonths(int requestedTenureMonths) { this.requestedTenureMonths = requestedTenureMonths; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public BigDecimal getSnapshotInterestRate() { return snapshotInterestRate; }
    public void setSnapshotInterestRate(BigDecimal snapshotInterestRate) { this.snapshotInterestRate = snapshotInterestRate; }

    public List<FeeDefinition> getSnapshotFeeSchedule() { return snapshotFeeSchedule; }
    public void setSnapshotFeeSchedule(List<FeeDefinition> snapshotFeeSchedule) { this.snapshotFeeSchedule = snapshotFeeSchedule; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
