package com.lending.origination.domain.event;

import com.lending.shared.domain.DomainEvent;

import java.util.UUID;

public record LoanApplicationRejectedEvent(
        UUID applicationId,
        UUID customerId,
        String reason
) implements DomainEvent {
}
