package com.lending.servicing.adapter.in.web;

import com.lending.servicing.domain.model.LoanState;
import com.lending.servicing.domain.service.RepaymentAllocationService;
import com.lending.shared.domain.Money;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RepaymentResponse(
        UUID id,
        UUID loanAccountId,
        Money amount,
        Instant receivedAt,
        List<AllocationResponse> allocations,
        Money remainingBalance,
        LoanState loanState,
        Instant createdAt
) {
    public record AllocationResponse(
            int installmentNumber,
            Money principalAllocated,
            Money interestAllocated
    ) {
    }

    public static RepaymentResponse from(RepaymentAllocationService.RepaymentResult result) {
        List<AllocationResponse> allocations = result.allocations().stream()
                .map(a -> new AllocationResponse(a.installmentNumber(), a.principalAllocated(), a.interestAllocated()))
                .toList();
        return new RepaymentResponse(
                result.repaymentId(), result.loanAccountId(), result.amount(),
                result.receivedAt(), allocations, result.remainingBalance(),
                result.loanState(), result.receivedAt()
        );
    }
}
