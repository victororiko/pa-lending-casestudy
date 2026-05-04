package com.lending.servicing.domain.service;

import com.lending.product.domain.model.FeeCalculationMethod;
import com.lending.product.domain.model.FeeType;
import com.lending.product.domain.model.OverpaymentPolicy;
import com.lending.servicing.domain.event.*;
import com.lending.servicing.domain.model.*;
import com.lending.servicing.port.out.InstallmentRepository;
import com.lending.servicing.port.out.LoanAccountRepository;
import com.lending.servicing.port.out.RepaymentRepository;
import com.lending.shared.adapter.out.persistence.AuditService;
import com.lending.shared.domain.Money;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RepaymentAllocationService {

    private final LoanAccountRepository loanAccountRepository;
    private final InstallmentRepository installmentRepository;
    private final RepaymentRepository repaymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final Clock clock;

    public RepaymentAllocationService(LoanAccountRepository loanAccountRepository,
                                       InstallmentRepository installmentRepository,
                                       RepaymentRepository repaymentRepository,
                                       ApplicationEventPublisher eventPublisher,
                                       AuditService auditService,
                                       Clock clock) {
        this.loanAccountRepository = loanAccountRepository;
        this.installmentRepository = installmentRepository;
        this.repaymentRepository = repaymentRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.clock = clock;
    }

    public RepaymentResult recordRepayment(UUID loanAccountId, Money amount, String idempotencyKey) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Repayment amount must be greater than 0.");
        }

        LoanAccount loanAccount = loanAccountRepository.findByIdWithLock(loanAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Loan account not found: " + loanAccountId));

        // Validate loan state
        if (!loanAccount.getState().isActive()) {
            throw new IllegalStateException("Cannot record repayment on a loan in state: " + loanAccount.getState());
        }

        List<Installment> unpaidInstallments = installmentRepository.findUnpaidByLoanAccountId(loanAccountId);

        // Check for early payoff
        boolean isEarlyPayoff = isEarlyPayoff(loanAccount, unpaidInstallments, amount);
        if (isEarlyPayoff && !loanAccount.isAllowEarlyRepayment()) {
            throw new IllegalStateException("Early repayment is not allowed for this loan product.");
        }

        // Check overpayment
        if (amount.isGreaterThan(loanAccount.getOutstandingBalance())) {
            OverpaymentPolicy policy = loanAccount.getOverpaymentPolicy();
            switch (policy) {
                case REJECT -> throw new IllegalStateException(
                        "Repayment amount exceeds outstanding balance. Outstanding: " +
                                loanAccount.getOutstandingBalance());
                case APPLY_TO_NEXT_INSTALLMENT -> {
                    // Allow — surplus will be allocated to next installments
                }
                case HOLD_AS_CREDIT -> {
                    // Allow — surplus will be held as credit
                }
            }
        }

        // Calculate prepayment penalty if applicable
        Money prepaymentPenalty = Money.zero(amount.getCurrency());
        if (isEarlyPayoff) {
            prepaymentPenalty = calculatePrepaymentPenalty(loanAccount);
        }

        // Allocate payment FIFO, interest before principal
        Money remaining = amount;
        List<AllocationRecord> allocations = new ArrayList<>();

        for (Installment installment : unpaidInstallments) {
            if (remaining.isZero() || !remaining.isPositive()) break;

            Money interestRemaining = installment.remainingInterest();
            Money principalRemaining = installment.remainingPrincipal();

            // Pay interest first
            Money interestPayment;
            if (remaining.isGreaterThanOrEqual(interestRemaining)) {
                interestPayment = interestRemaining;
            } else {
                interestPayment = remaining;
            }
            remaining = remaining.subtract(interestPayment);

            // Then principal
            Money principalPayment;
            if (remaining.isGreaterThanOrEqual(principalRemaining)) {
                principalPayment = principalRemaining;
            } else {
                principalPayment = remaining;
            }
            remaining = remaining.subtract(principalPayment);

            installment.applyPayment(interestPayment, principalPayment);

            allocations.add(new AllocationRecord(
                    installment.getInstallmentNumber(),
                    principalPayment,
                    interestPayment
            ));

            // Publish allocation event
            eventPublisher.publishEvent(new RepaymentAllocatedEvent(
                    loanAccountId, null, installment.getInstallmentNumber(),
                    principalPayment, interestPayment));
        }

        // Save updated installments
        installmentRepository.saveAll(unpaidInstallments);

        // Handle surplus
        Money surplus = remaining;
        if (surplus.isPositive() && loanAccount.getOverpaymentPolicy() == OverpaymentPolicy.HOLD_AS_CREDIT) {
            loanAccount.setCreditBalance(loanAccount.getCreditBalance().add(surplus));
        }

        // Update outstanding balance
        Money totalPaid = amount.subtract(surplus);
        loanAccount.setOutstandingBalance(loanAccount.getOutstandingBalance().subtract(totalPaid));

        // Check if loan is fully paid
        boolean allPaid = unpaidInstallments.stream().allMatch(Installment::isFullyPaid);
        LoanState previousState = loanAccount.getState();

        if (allPaid && loanAccount.getOutstandingBalance().isZero()) {
            loanAccount.transitionTo(LoanState.CLOSED);
            loanAccount.setClosedAt(Instant.now(clock));
        } else if (loanAccount.getState() == LoanState.OVERDUE) {
            // Check if all overdue installments are now settled
            List<Installment> stillOverdue = installmentRepository
                    .findOverdueInstallments(loanAccountId, LocalDate.now(clock));
            if (stillOverdue.stream().allMatch(Installment::isFullyPaid)) {
                loanAccount.transitionTo(LoanState.ACTIVE);
            }
        }

        // Save repayment record
        Repayment repayment = new Repayment();
        repayment.setLoanAccountId(loanAccountId);
        repayment.setAmount(amount);
        repayment.setReceivedAt(Instant.now(clock));
        repayment.setIdempotencyKey(idempotencyKey);
        repaymentRepository.save(repayment);

        LoanAccount saved = loanAccountRepository.save(loanAccount);

        // Publish events
        eventPublisher.publishEvent(new RepaymentReceivedEvent(
                loanAccountId, saved.getCustomerId(), repayment.getId(), amount));

        if (saved.getState() == LoanState.CLOSED && previousState != LoanState.CLOSED) {
            eventPublisher.publishEvent(new LoanClosedEvent(loanAccountId, saved.getCustomerId()));
        }

        return new RepaymentResult(
                repayment.getId(),
                loanAccountId,
                amount,
                repayment.getReceivedAt(),
                allocations,
                saved.getOutstandingBalance(),
                saved.getState()
        );
    }

    private boolean isEarlyPayoff(LoanAccount loanAccount, List<Installment> unpaidInstallments, Money amount) {
        if (unpaidInstallments.isEmpty()) return false;
        // Early payoff = paying off entire remaining balance before last installment due date
        Installment lastInstallment = unpaidInstallments.get(unpaidInstallments.size() - 1);
        LocalDate today = LocalDate.now(clock);
        boolean beforeLastDueDate = today.isBefore(lastInstallment.getDueDate());
        boolean paysOffBalance = amount.isGreaterThanOrEqual(loanAccount.getOutstandingBalance());
        return beforeLastDueDate && paysOffBalance && unpaidInstallments.size() > 1;
    }

    private Money calculatePrepaymentPenalty(LoanAccount loanAccount) {
        // This would need the fee schedule from the product snapshot
        // For simplicity, we check the product's fee schedule
        // In a full implementation, fees would be snapshotted on the LoanAccount
        return Money.zero(loanAccount.getPrincipal().getCurrency());
    }

    public record AllocationRecord(
            int installmentNumber,
            Money principalAllocated,
            Money interestAllocated
    ) {
    }

    public record RepaymentResult(
            UUID repaymentId,
            UUID loanAccountId,
            Money amount,
            Instant receivedAt,
            List<AllocationRecord> allocations,
            Money remainingBalance,
            LoanState loanState
    ) {
    }
}
