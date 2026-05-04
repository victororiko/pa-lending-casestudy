package com.lending.customer.domain.service;

import com.lending.customer.adapter.in.web.CreateCustomerRequest;
import com.lending.customer.domain.event.CustomerCreatedEvent;
import com.lending.customer.domain.event.CustomerUpdatedEvent;
import com.lending.customer.domain.model.Customer;
import com.lending.customer.port.out.CustomerRepository;
import com.lending.shared.adapter.out.persistence.AuditService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public CustomerService(CustomerRepository customerRepository, AuditService auditService,
                           ApplicationEventPublisher eventPublisher) {
        this.customerRepository = customerRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public Customer createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("A customer with email '" + request.email() + "' already exists.");
        }
        if (customerRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new IllegalStateException("A customer with phone number '" + request.phoneNumber() + "' already exists.");
        }
        if (customerRepository.existsByNationalId(request.nationalId())) {
            throw new IllegalStateException("A customer with national ID '" + request.nationalId() + "' already exists.");
        }

        Customer customer = new Customer();
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhoneNumber(request.phoneNumber());
        customer.setNationalId(request.nationalId());
        customer.setCreditLimit(request.creditLimit());
        customer.setMaxActiveLoans(request.maxActiveLoans() > 0 ? request.maxActiveLoans() : 3);
        customer.validate();

        Customer saved = customerRepository.save(customer);
        auditService.logCreate("Customer", saved.getId(), saved, "system");
        eventPublisher.publishEvent(new CustomerCreatedEvent(saved.getId(), saved.getEmail()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(UUID id) {
        return customerRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Customer> listCustomers(Pageable pageable) {
        return customerRepository.findAllActive(pageable);
    }

    public Customer updateCustomer(UUID id, CreateCustomerRequest request) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));

        if (existing.isDeleted()) {
            throw new EntityNotFoundException("Customer not found: " + id);
        }

        existing.setFirstName(request.firstName());
        existing.setLastName(request.lastName());
        existing.setEmail(request.email());
        existing.setPhoneNumber(request.phoneNumber());
        existing.setNationalId(request.nationalId());
        existing.setCreditLimit(request.creditLimit());
        existing.setMaxActiveLoans(request.maxActiveLoans() > 0 ? request.maxActiveLoans() : 3);
        existing.validate();

        Customer saved = customerRepository.save(existing);
        auditService.logUpdate("Customer", saved.getId(), existing, saved, "system");
        eventPublisher.publishEvent(new CustomerUpdatedEvent(saved.getId(), saved.getEmail()));
        return saved;
    }
}
