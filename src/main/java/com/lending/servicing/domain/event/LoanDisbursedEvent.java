package com.lending.servicing.domain.event;

import com.lending.shared.domain.DomainEvent;
import com.lending.shared.domain.Money;

import java.util.UUID;

public record LoanDisbursedEvent(
        UUID loanAccountId,
        UUID customerId,
        Money principalDisbursed,
        Money originationFee
) implements DomainEvent {
}
