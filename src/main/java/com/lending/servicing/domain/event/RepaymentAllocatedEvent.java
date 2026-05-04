package com.lending.servicing.domain.event;

import com.lending.shared.domain.DomainEvent;
import com.lending.shared.domain.Money;

import java.util.UUID;

public record RepaymentAllocatedEvent(
        UUID loanAccountId,
        UUID repaymentId,
        int installmentNumber,
        Money principalAllocated,
        Money interestAllocated
) implements DomainEvent {
}
