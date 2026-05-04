package com.lending.origination.domain.event;

import com.lending.shared.domain.DomainEvent;

import java.util.UUID;

public record LoanApplicationApprovedEvent(
        UUID applicationId,
        UUID customerId,
        UUID productId
) implements DomainEvent {
}
