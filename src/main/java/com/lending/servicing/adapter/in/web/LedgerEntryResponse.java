package com.lending.servicing.adapter.in.web;

import com.lending.ledger.domain.model.LedgerEntry;
import com.lending.ledger.domain.model.LedgerEntryType;
import com.lending.ledger.domain.model.Direction;
import com.lending.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID loanAccountId,
        LedgerEntryType entryType,
        Money amount,
        Direction direction,
        String description,
        UUID referenceId,
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry e) {
        return new LedgerEntryResponse(
                e.getId(), e.getLoanAccountId(), e.getEntryType(),
                e.getAmount(), e.getDirection(), e.getDescription(),
                e.getReferenceId(), e.getCreatedAt()
        );
    }
}
