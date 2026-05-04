package com.lending.ledger.domain.model;

import com.lending.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public class LedgerEntry {

    private UUID id;
    private UUID loanAccountId;
    private LedgerEntryType entryType;
    private Money amount;
    private Direction direction;
    private String description;
    private UUID referenceId;
    private Instant createdAt;

    public LedgerEntry() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public LedgerEntry(UUID loanAccountId, LedgerEntryType entryType, Money amount,
                        Direction direction, String description, UUID referenceId) {
        this();
        this.loanAccountId = loanAccountId;
        this.entryType = entryType;
        this.amount = amount;
        this.direction = direction;
        this.description = description;
        this.referenceId = referenceId;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getLoanAccountId() { return loanAccountId; }
    public void setLoanAccountId(UUID v) { this.loanAccountId = v; }
    public LedgerEntryType getEntryType() { return entryType; }
    public void setEntryType(LedgerEntryType v) { this.entryType = v; }
    public Money getAmount() { return amount; }
    public void setAmount(Money v) { this.amount = v; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction v) { this.direction = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID v) { this.referenceId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
