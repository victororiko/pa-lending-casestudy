package com.lending.servicing.domain.event;

import com.lending.shared.domain.DomainEvent;

import java.util.UUID;

public record LoanDefaultedEvent(
        UUID loanAccountId,
        UUID customerId
) implements DomainEvent {
}
