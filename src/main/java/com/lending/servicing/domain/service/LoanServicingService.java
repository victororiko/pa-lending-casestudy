package com.lending.servicing.domain.service;

import com.lending.origination.domain.event.LoanApplicationApprovedEvent;
import com.lending.origination.domain.model.LoanApplication;
import com.lending.origination.port.out.LoanApplicationRepository;
import com.lending.product.domain.model.*;
import com.lending.product.port.out.LoanProductRepository;
import com.lending.servicing.domain.event.LoanDisbursedEvent;
import com.lending.servicing.domain.model.*;
import com.lending.servicing.port.out.InstallmentRepository;
import com.lending.servicing.port.out.LoanAccountRepository;
import com.lending.shared.adapter.out.persistence.AuditService;
import com.lending.shared.domain.Money;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LoanServicingService {

    private final LoanAccountRepository loanAccountRepository;
    private final InstallmentRepository installmentRepository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final Clock clock;

    public LoanServicingService(LoanAccountRepository loanAccountRepository,
                                 InstallmentRepository installmentRepository,
                                 LoanApplicationRepository applicationRepository,
                                 LoanProductRepository productRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 AuditService auditService,
                                 Clock clock) {
        this.loanAccountRepository = loanAccountRepository;
        this.installmentRepository = installmentRepository;
        this.applicationRepository = applicationRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.clock = clock;
    }

    @EventListener
    public void onApplicationApproved(LoanApplicationApprovedEvent event) {
        createLoanAccountFromApplication(event.applicationId());
    }

    public LoanAccount createLoanAccountFromApplication(UUID applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + applicationId));

        LoanProduct product = productRepository.findById(application.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + application.getProductId()));

        // Compute origination fee
        Money originationFee = computeOriginationFee(product, application.getRequestedPrincipal());
        Currency currency = application.getRequestedPrincipal().getCurrency();

        // Create the LoanAccount with snapshotted parameters
        LoanAccount loanAccount = new LoanAccount();
        loanAccount.setApplicationId(applicationId);
        loanAccount.setCustomerId(application.getCustomerId());
        loanAccount.setProductId(application.getProductId());
        loanAccount.setPrincipal(application.getRequestedPrincipal());
        loanAccount.setInterestRatePerAnnum(application.getSnapshotInterestRate() != null
                ? application.getSnapshotInterestRate() : product.getInterestRatePerAnnum());
        loanAccount.setInterestAccrualMethod(product.getInterestAccrualMethod());
        loanAccount.setTenureMonths(application.getRequestedTenureMonths());
        loanAccount.setRepaymentFrequency(product.getRepaymentFrequency());
        loanAccount.setGracePeriodDays(product.getGracePeriodDays());
        loanAccount.setOriginationFeeChargeMethod(product.getOriginationFeeChargeMethod());
        loanAccount.setAllowEarlyRepayment(product.isAllowEarlyRepayment());
        loanAccount.setOverpaymentPolicy(product.getOverpaymentPolicy());
        loanAccount.setOriginationFee(originationFee);
        loanAccount.setCreditBalance(Money.zero(currency));

        // Calculate outstanding balance based on origination fee charge method
        Money outstandingBalance;
        Money totalDisbursed;
        if (product.getOriginationFeeChargeMethod() == OriginationFeeChargeMethod.ADDED_TO_BALANCE) {
            totalDisbursed = application.getRequestedPrincipal();
            outstandingBalance = application.getRequestedPrincipal(); // Fee will be added after schedule generation
        } else {
            totalDisbursed = application.getRequestedPrincipal().subtract(originationFee);
            outstandingBalance = application.getRequestedPrincipal(); // Schedule is based on full principal
        }
        loanAccount.setTotalDisbursed(totalDisbursed);
        loanAccount.setOutstandingBalance(outstandingBalance); // Will be set properly after schedule
        loanAccount.setState(LoanState.PENDING_DISBURSEMENT);

        LoanAccount saved = loanAccountRepository.save(loanAccount);
        auditService.logCreate("LoanAccount", saved.getId(), saved, "system");
        return saved;
    }

    public LoanAccount disburseLoan(UUID loanAccountId) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Loan account not found: " + loanAccountId));

        // Check if DAILY_ACCRUAL — not implemented in v1
        if (loanAccount.getInterestAccrualMethod() == InterestAccrualMethod.DAILY_ACCRUAL) {
            throw new UnsupportedOperationException(
                    "DAILY_ACCRUAL interest method is not implemented in v1. Cannot disburse.");
        }

        loanAccount.transitionTo(LoanState.ACTIVE);
        loanAccount.setDisbursedAt(Instant.now(clock));

        // Generate amortisation schedule
        LocalDate disbursementDate = LocalDate.now(clock);
        List<Installment> installments = AmortisationCalculator.generateSchedule(
                loanAccount.getId(),
                loanAccount.getPrincipal(),
                loanAccount.getInterestRatePerAnnum(),
                loanAccount.getTenureMonths(),
                loanAccount.getRepaymentFrequency(),
                disbursementDate
        );

        // Calculate total outstanding balance (sum of all installment totalDue)
        Money totalOutstanding = installments.stream()
                .map(Installment::getTotalDue)
                .reduce(Money::add)
                .orElse(Money.zero(loanAccount.getPrincipal().getCurrency()));

        // If origination fee is ADDED_TO_BALANCE, add it to outstanding
        if (loanAccount.getOriginationFeeChargeMethod() == OriginationFeeChargeMethod.ADDED_TO_BALANCE) {
            totalOutstanding = totalOutstanding.add(loanAccount.getOriginationFee());
        }

        loanAccount.setOutstandingBalance(totalOutstanding);

        LoanAccount saved = loanAccountRepository.save(loanAccount);
        installmentRepository.saveAll(installments);

        auditService.logStateChange("LoanAccount", saved.getId(),
                "PENDING_DISBURSEMENT", "ACTIVE", "system");

        eventPublisher.publishEvent(new LoanDisbursedEvent(
                saved.getId(), saved.getCustomerId(),
                saved.getTotalDisbursed(), saved.getOriginationFee()));

        return saved;
    }

    @Transactional(readOnly = true)
    public LoanAccount getLoanAccount(UUID id) {
        return loanAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan account not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Installment> getSchedule(UUID loanAccountId) {
        loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Loan account not found: " + loanAccountId));
        return installmentRepository.findByLoanAccountId(loanAccountId);
    }

    @Transactional(readOnly = true)
    public List<LoanAccount> getCustomerLoans(UUID customerId) {
        return loanAccountRepository.findByCustomerId(customerId);
    }

    private Money computeOriginationFee(LoanProduct product, Money principal) {
        FeeDefinition originationFee = product.getFeeSchedule().stream()
                .filter(f -> f.getFeeType() == FeeType.ORIGINATION_FEE)
                .findFirst()
                .orElse(null);

        if (originationFee == null) {
            return Money.zero(principal.getCurrency());
        }

        return switch (originationFee.getCalculationMethod()) {
            case FLAT -> originationFee.getAmount();
            case PERCENTAGE_OF_PRINCIPAL -> principal.multiply(
                    originationFee.getAmount().getAmount().divide(BigDecimal.valueOf(100),
                            Money.SCALE, Money.ROUNDING));
            default -> Money.zero(principal.getCurrency());
        };
    }
}
