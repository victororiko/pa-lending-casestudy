package com.lending.servicing.adapter.in.web;

import com.lending.servicing.domain.model.Installment;
import com.lending.servicing.domain.model.InstallmentStatus;
import com.lending.shared.domain.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleResponse(
        UUID loanAccountId,
        List<InstallmentResponse> installments
) {
    public record InstallmentResponse(
            int installmentNumber,
            LocalDate dueDate,
            Money principalDue,
            Money interestDue,
            Money totalDue,
            Money principalPaid,
            Money interestPaid,
            Money totalPaid,
            InstallmentStatus status
    ) {
        public static InstallmentResponse from(Installment i) {
            return new InstallmentResponse(
                    i.getInstallmentNumber(), i.getDueDate(),
                    i.getPrincipalDue(), i.getInterestDue(), i.getTotalDue(),
                    i.getPrincipalPaid(), i.getInterestPaid(), i.getTotalPaid(),
                    i.getStatus()
            );
        }
    }
}
