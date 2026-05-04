package com.lending.customer.domain.event;

import com.lending.shared.domain.DomainEvent;

import java.util.UUID;

public record CustomerUpdatedEvent(UUID customerId, String email) implements DomainEvent {
}
