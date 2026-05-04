package com.lending.servicing.domain.service;

import com.lending.ledger.domain.model.Direction;
import com.lending.ledger.domain.model.LedgerEntry;
import com.lending.ledger.domain.model.LedgerEntryType;
import com.lending.ledger.port.out.LedgerEntryRepository;
import com.lending.product.domain.model.FeeCalculationMethod;
import com.lending.product.domain.model.FeeDefinition;
import com.lending.product.domain.model.FeeType;
import com.lending.product.domain.model.LoanProduct;
import com.lending.product.port.out.LoanProductRepository;
import com.lending.servicing.domain.event.LoanOverdueEvent;
import com.lending.servicing.domain.model.*;
import com.lending.servicing.port.out.InstallmentRepository;
import com.lending.servicing.port.out.LoanAccountRepository;
import com.lending.shared.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class OverdueDetectionService {

    private static final Logger log = LoggerFactory.getLogger(OverdueDetectionService.class);

    private final LoanAccountRepository loanAccountRepository;
    private final InstallmentRepository installmentRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LoanProductRepository loanProductRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public OverdueDetectionService(LoanAccountRepository loanAccountRepository,
                                    InstallmentRepository installmentRepository,
                                    LedgerEntryRepository ledgerEntryRepository,
                                    LoanProductRepository loanProductRepository,
                                    ApplicationEventPublisher eventPublisher,
                                    Clock clock) {
        this.loanAccountRepository = loanAccountRepository;
        this.installmentRepository = installmentRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.loanProductRepository = loanProductRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM UTC
    @Transactional
    public void detectOverdueLoans() {
        log.info("Starting overdue detection job");
        LocalDate today = LocalDate.now(clock);

        List<LoanAccount> activeLoans = loanAccountRepository.findActiveAndOverdueLoans();

        int overdueCount = 0;
        for (LoanAccount loan : activeLoans) {
            boolean newlyOverdue = processLoan(loan, today);
            if (newlyOverdue) overdueCount++;
        }

        log.info("Overdue detection complete. {} loans transitioned to OVERDUE", overdueCount);
    }

    @Transactional
    public boolean processLoan(LoanAccount loan, LocalDate today) {
        // Calculate the date threshold considering grace period
        // An installment is overdue if: today > dueDate + gracePeriodDays
        LocalDate graceCutoff = today.minusDays(loan.getGracePeriodDays());

        List<Installment> overdueInstallments = installmentRepository
                .findOverdueInstallments(loan.getId(), graceCutoff);

        if (overdueInstallments.isEmpty()) {
            return false;
        }

        // Mark installments as OVERDUE
        boolean anyChanged = false;
        for (Installment installment : overdueInstallments) {
            if (installment.getStatus() != InstallmentStatus.OVERDUE &&
                    installment.getStatus() != InstallmentStatus.PAID) {
                installment.setStatus(InstallmentStatus.OVERDUE);
                anyChanged = true;
            }
        }

        if (anyChanged) {
            installmentRepository.saveAll(overdueInstallments);
        }

        // Transition loan state to OVERDUE if currently ACTIVE
        if (loan.getState() == LoanState.ACTIVE) {
            loan.transitionTo(LoanState.OVERDUE);

            // Apply late fee per overdue installment if configured on the product
            applyLateFees(loan, overdueInstallments);

            loanAccountRepository.save(loan);

            eventPublisher.publishEvent(new LoanOverdueEvent(loan.getId(), loan.getCustomerId()));

            log.info("Loan {} transitioned to OVERDUE ({} overdue installments)",
                    loan.getId(), overdueInstallments.size());
            return true;
        }

        return false;
    }

    private void applyLateFees(LoanAccount loan, List<Installment> overdueInstallments) {
        LoanProduct product = loanProductRepository.findById(loan.getProductId()).orElse(null);
        if (product == null) return;

        FeeDefinition lateFee = product.getFeeSchedule().stream()
                .filter(f -> f.getFeeType() == FeeType.LATE_PAYMENT_FEE)
                .findFirst()
                .orElse(null);

        if (lateFee == null) return;

        Money feeAmount = lateFee.getAmount();
        // Apply fee per overdue installment
        for (Installment installment : overdueInstallments) {
            // Record late fee in ledger
            ledgerEntryRepository.save(new LedgerEntry(
                    loan.getId(),
                    LedgerEntryType.FEE_CHARGE,
                    feeAmount,
                    Direction.CREDIT,
                    "Late payment fee - installment " + installment.getInstallmentNumber(),
                    null
            ));
            // Add fee to outstanding balance
            loan.setOutstandingBalance(loan.getOutstandingBalance().add(feeAmount));
        }

        log.info("Applied {} late fee(s) of {} to loan {}", overdueInstallments.size(),
                feeAmount, loan.getId());
    }
}
