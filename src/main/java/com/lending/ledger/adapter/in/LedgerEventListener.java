package com.lending.ledger.adapter.in;

import com.lending.ledger.domain.model.Direction;
import com.lending.ledger.domain.model.LedgerEntry;
import com.lending.ledger.domain.model.LedgerEntryType;
import com.lending.ledger.port.out.LedgerEntryRepository;
import com.lending.servicing.domain.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LedgerEventListener {

    private static final Logger log = LoggerFactory.getLogger(LedgerEventListener.class);
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerEventListener(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @EventListener
    public void onLoanDisbursed(LoanDisbursedEvent event) {
        log.info("LEDGER: Recording disbursement for loan {}", event.loanAccountId());

        ledgerEntryRepository.save(new LedgerEntry(
                event.loanAccountId(),
                LedgerEntryType.DISBURSEMENT,
                event.principalDisbursed(),
                Direction.DEBIT,
                "Loan disbursement",
                null
        ));

        if (event.originationFee().isPositive()) {
            ledgerEntryRepository.save(new LedgerEntry(
                    event.loanAccountId(),
                    LedgerEntryType.FEE_CHARGE,
                    event.originationFee(),
                    Direction.CREDIT,
                    "Origination fee",
                    null
            ));
        }
    }

    @EventListener
    public void onRepaymentAllocated(RepaymentAllocatedEvent event) {
        log.info("LEDGER: Recording repayment allocation for loan {} installment {}",
                event.loanAccountId(), event.installmentNumber());

        if (event.principalAllocated().isPositive()) {
            ledgerEntryRepository.save(new LedgerEntry(
                    event.loanAccountId(),
                    LedgerEntryType.REPAYMENT_PRINCIPAL,
                    event.principalAllocated(),
                    Direction.CREDIT,
                    "Principal repayment - installment " + event.installmentNumber(),
                    event.repaymentId()
            ));
        }

        if (event.interestAllocated().isPositive()) {
            ledgerEntryRepository.save(new LedgerEntry(
                    event.loanAccountId(),
                    LedgerEntryType.REPAYMENT_INTEREST,
                    event.interestAllocated(),
                    Direction.CREDIT,
                    "Interest repayment - installment " + event.installmentNumber(),
                    event.repaymentId()
            ));
        }
    }

    @EventListener
    public void onLoanOverdue(LoanOverdueEvent event) {
        log.info("LEDGER: Loan {} marked as overdue", event.loanAccountId());
    }
}
