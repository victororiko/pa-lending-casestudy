package com.lending.servicing.domain.event;

import com.lending.shared.domain.DomainEvent;
import com.lending.shared.domain.Money;

import java.util.UUID;

public record RepaymentReceivedEvent(
        UUID loanAccountId,
        UUID customerId,
        UUID repaymentId,
        Money amount
) implements DomainEvent {
}
