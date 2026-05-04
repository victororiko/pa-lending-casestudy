package com.lending.origination.domain.service;

import com.lending.customer.domain.model.Customer;
import com.lending.customer.port.out.CustomerRepository;
import com.lending.origination.adapter.in.web.SubmitApplicationRequest;
import com.lending.origination.domain.event.LoanApplicationApprovedEvent;
import com.lending.origination.domain.event.LoanApplicationRejectedEvent;
import com.lending.origination.domain.event.LoanApplicationSubmittedEvent;
import com.lending.origination.domain.model.LoanApplication;
import com.lending.origination.port.out.LoanApplicationRepository;
import com.lending.product.domain.model.LoanProduct;
import com.lending.product.port.out.LoanProductRepository;
import com.lending.servicing.port.out.LoanAccountRepository;
import com.lending.shared.adapter.out.persistence.AuditService;
import com.lending.shared.domain.Money;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    public LoanApplicationService(LoanApplicationRepository applicationRepository,
                                   LoanProductRepository productRepository,
                                   CustomerRepository customerRepository,
                                   LoanAccountRepository loanAccountRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    public LoanApplication submitApplication(SubmitApplicationRequest request) {
        // Validate product exists
        LoanProduct product = productRepository.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        // Validate customer exists
        customerRepository.findById(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.customerId()));

        // Basic validation (product active, principal/tenure in range)
        if (!product.isActive() || product.isDeleted()) {
            throw new IllegalArgumentException("Product is not active.");
        }
        if (request.requestedPrincipal().isLessThan(product.getMinPrincipal())) {
            throw new IllegalArgumentException("Requested principal is below the minimum.");
        }
        if (request.requestedPrincipal().isGreaterThan(product.getMaxPrincipal())) {
            throw new IllegalArgumentException("Requested principal exceeds the maximum.");
        }
        if (request.requestedTenureMonths() < product.getMinTenureMonths()) {
            throw new IllegalArgumentException("Requested tenure is below the minimum.");
        }
        if (request.requestedTenureMonths() > product.getMaxTenureMonths()) {
            throw new IllegalArgumentException("Requested tenure exceeds the maximum.");
        }

        LoanApplication application = new LoanApplication();
        application.setCustomerId(request.customerId());
        application.setProductId(request.productId());
        application.setRequestedPrincipal(request.requestedPrincipal());
        application.setRequestedTenureMonths(request.requestedTenureMonths());

        LoanApplication saved = applicationRepository.save(application);
        auditService.logCreate("LoanApplication", saved.getId(), saved, "system");
        eventPublisher.publishEvent(new LoanApplicationSubmittedEvent(
                saved.getId(), saved.getCustomerId(), saved.getProductId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public LoanApplication getApplication(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + id));
    }

    public LoanApplication approveApplication(UUID id) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + id));

        LoanProduct product = productRepository.findById(application.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + application.getProductId()));

        Customer customer = customerRepository.findById(application.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + application.getCustomerId()));

        // Full eligibility check
        Money outstandingPrincipal = loanAccountRepository
                .sumOutstandingPrincipalByCustomer(application.getCustomerId());
        int activeLoansCount = loanAccountRepository
                .countActiveByCustomer(application.getCustomerId());

        EligibilityValidator.EligibilityResult result = EligibilityValidator.validate(
                product, customer, application.getRequestedPrincipal(),
                application.getRequestedTenureMonths(), outstandingPrincipal, activeLoansCount);

        if (!result.eligible()) {
            application.reject(result.rejectionReason());
            LoanApplication saved = applicationRepository.save(application);
            eventPublisher.publishEvent(new LoanApplicationRejectedEvent(
                    saved.getId(), saved.getCustomerId(), result.rejectionReason()));
            throw new IllegalStateException("Eligibility check failed: " + result.rejectionReason());
        }

        // Snapshot product params and approve
        application.approve(product.getInterestRatePerAnnum(), product.getFeeSchedule());
        LoanApplication saved = applicationRepository.save(application);

        auditService.logStateChange("LoanApplication", saved.getId(), "PENDING", "APPROVED", "system");

        eventPublisher.publishEvent(new LoanApplicationApprovedEvent(
                saved.getId(), saved.getCustomerId(), saved.getProductId()));

        return saved;
    }

    public LoanApplication rejectApplication(UUID id, String reason) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + id));

        application.reject(reason);
        LoanApplication saved = applicationRepository.save(application);

        auditService.logStateChange("LoanApplication", saved.getId(), "PENDING", "REJECTED", "system");

        eventPublisher.publishEvent(new LoanApplicationRejectedEvent(
                saved.getId(), saved.getCustomerId(), reason));

        return saved;
    }
}
