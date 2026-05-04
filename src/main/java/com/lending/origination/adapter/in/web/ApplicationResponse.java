package com.lending.origination.adapter.in.web;

import com.lending.origination.domain.model.ApplicationStatus;
import com.lending.origination.domain.model.LoanApplication;
import com.lending.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID customerId,
        UUID productId,
        Money requestedPrincipal,
        int requestedTenureMonths,
        ApplicationStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public static ApplicationResponse from(LoanApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getCustomerId(),
                app.getProductId(),
                app.getRequestedPrincipal(),
                app.getRequestedTenureMonths(),
                app.getStatus(),
                app.getRejectionReason(),
                app.getCreatedAt(),
                app.getUpdatedAt(),
                app.getVersion()
        );
    }
}
