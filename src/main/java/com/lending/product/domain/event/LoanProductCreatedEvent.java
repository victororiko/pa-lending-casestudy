package com.lending.product.domain.event;

import com.lending.shared.domain.DomainEvent;

import java.util.UUID;

public record LoanProductCreatedEvent(UUID productId, String productName) implements DomainEvent {
}
