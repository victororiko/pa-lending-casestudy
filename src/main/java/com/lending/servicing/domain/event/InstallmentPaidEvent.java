package com.lending.servicing.domain.event;

import com.lending.shared.domain.DomainEvent;

import java.util.UUID;

public record InstallmentPaidEvent(
        UUID loanAccountId,
        int installmentNumber
) implements DomainEvent {
}
