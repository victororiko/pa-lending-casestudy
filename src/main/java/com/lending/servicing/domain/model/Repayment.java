package com.lending.servicing.domain.model;

import com.lending.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public class Repayment {

    private UUID id;
    private UUID loanAccountId;
    private Money amount;
    private Instant receivedAt;
    private String idempotencyKey;
    private Instant createdAt;

    public Repayment() {
        this.id = UUID.randomUUID();
        this.receivedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLoanAccountId() { return loanAccountId; }
    public void setLoanAccountId(UUID loanAccountId) { this.loanAccountId = loanAccountId; }

    public Money getAmount() { return amount; }
    public void setAmount(Money amount) { this.amount = amount; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
