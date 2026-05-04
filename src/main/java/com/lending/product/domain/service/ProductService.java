package com.lending.product.domain.service;

import com.lending.product.adapter.in.web.CreateProductRequest;
import com.lending.product.domain.event.LoanProductCreatedEvent;
import com.lending.product.domain.event.LoanProductUpdatedEvent;
import com.lending.product.domain.model.*;
import com.lending.product.port.in.CreateProductUseCase;
import com.lending.product.port.in.GetProductUseCase;
import com.lending.product.port.in.UpdateProductUseCase;
import com.lending.product.port.out.LoanProductRepository;
import com.lending.shared.adapter.out.persistence.AuditService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService implements CreateProductUseCase, GetProductUseCase, UpdateProductUseCase {

    private final LoanProductRepository productRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(LoanProductRepository productRepository,
                          AuditService auditService,
                          ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public LoanProduct createProduct(CreateProductRequest request) {
        if (productRepository.existsByName(request.name())) {
            throw new IllegalStateException("A product with the name '" + request.name() + "' already exists.");
        }

        LoanProduct product = mapRequestToDomain(request);
        product.validate();

        LoanProduct saved = productRepository.save(product);
        auditService.logCreate("LoanProduct", saved.getId(), saved, "system");
        eventPublisher.publishEvent(new LoanProductCreatedEvent(saved.getId(), saved.getName()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public LoanProduct getProduct(UUID id) {
        return productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanProduct> listProducts(Pageable pageable) {
        return productRepository.findAllActive(pageable);
    }

    @Override
    public LoanProduct updateProduct(UUID id, CreateProductRequest request) {
        LoanProduct existing = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

        if (existing.isDeleted()) {
            throw new EntityNotFoundException("Product not found: " + id);
        }

        LoanProduct before = copyForAudit(existing);

        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setInterestRatePerAnnum(request.interestRatePerAnnum());
        existing.setInterestAccrualMethod(
                request.interestAccrualMethod() != null ? request.interestAccrualMethod() : InterestAccrualMethod.PRE_COMPUTED);
        existing.setMinTenureMonths(request.minTenureMonths());
        existing.setMaxTenureMonths(request.maxTenureMonths());
        existing.setMinPrincipal(request.minPrincipal());
        existing.setMaxPrincipal(request.maxPrincipal());
        existing.setRepaymentFrequency(request.repaymentFrequency());
        existing.setGracePeriodDays(request.gracePeriodDays());
        existing.setOriginationFeeChargeMethod(
                request.originationFeeChargeMethod() != null ? request.originationFeeChargeMethod()
                        : OriginationFeeChargeMethod.DEDUCTED_FROM_DISBURSEMENT);
        existing.setAllowEarlyRepayment(request.allowEarlyRepayment());
        existing.setOverpaymentPolicy(
                request.overpaymentPolicy() != null ? request.overpaymentPolicy() : OverpaymentPolicy.REJECT);

        List<FeeDefinition> fees = mapFees(request.feeSchedule());
        existing.setFeeSchedule(fees);

        existing.validate();

        LoanProduct saved = productRepository.save(existing);
        auditService.logUpdate("LoanProduct", saved.getId(), before, saved, "system");
        eventPublisher.publishEvent(new LoanProductUpdatedEvent(saved.getId(), saved.getName()));
        return saved;
    }

    @Override
    public void deleteProduct(UUID id) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

        if (product.isDeleted()) {
            throw new EntityNotFoundException("Product not found: " + id);
        }

        product.softDelete();
        productRepository.save(product);
        auditService.logDelete("LoanProduct", product.getId(), product, "system");
    }

    private LoanProduct mapRequestToDomain(CreateProductRequest request) {
        LoanProduct product = new LoanProduct();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setInterestRatePerAnnum(request.interestRatePerAnnum());
        product.setInterestAccrualMethod(
                request.interestAccrualMethod() != null ? request.interestAccrualMethod() : InterestAccrualMethod.PRE_COMPUTED);
        product.setMinTenureMonths(request.minTenureMonths());
        product.setMaxTenureMonths(request.maxTenureMonths());
        product.setMinPrincipal(request.minPrincipal());
        product.setMaxPrincipal(request.maxPrincipal());
        product.setRepaymentFrequency(request.repaymentFrequency());
        product.setGracePeriodDays(request.gracePeriodDays());
        product.setOriginationFeeChargeMethod(
                request.originationFeeChargeMethod() != null ? request.originationFeeChargeMethod()
                        : OriginationFeeChargeMethod.DEDUCTED_FROM_DISBURSEMENT);
        product.setAllowEarlyRepayment(request.allowEarlyRepayment());
        product.setOverpaymentPolicy(
                request.overpaymentPolicy() != null ? request.overpaymentPolicy() : OverpaymentPolicy.REJECT);

        List<FeeDefinition> fees = mapFees(request.feeSchedule());
        product.setFeeSchedule(fees);

        return product;
    }

    private List<FeeDefinition> mapFees(List<CreateProductRequest.FeeDefinitionRequest> feeRequests) {
        if (feeRequests == null) return List.of();
        return feeRequests.stream()
                .map(fr -> new FeeDefinition(UUID.randomUUID(), fr.feeType(), fr.calculationMethod(), fr.amount()))
                .collect(Collectors.toList());
    }

    private LoanProduct copyForAudit(LoanProduct product) {
        LoanProduct copy = new LoanProduct();
        copy.setId(product.getId());
        copy.setName(product.getName());
        copy.setInterestRatePerAnnum(product.getInterestRatePerAnnum());
        copy.setActive(product.isActive());
        return copy;
    }
}
