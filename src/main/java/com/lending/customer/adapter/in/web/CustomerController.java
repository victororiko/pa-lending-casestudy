package com.lending.customer.adapter.in.web;

import com.lending.customer.domain.model.Customer;
import com.lending.customer.domain.service.CustomerService;
import com.lending.servicing.adapter.in.web.LoanAccountResponse;
import com.lending.servicing.domain.model.LoanAccount;
import com.lending.servicing.domain.service.LoanServicingService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final LoanServicingService loanServicingService;

    public CustomerController(CustomerService customerService, LoanServicingService loanServicingService) {
        this.customerService = customerService;
        this.loanServicingService = loanServicingService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> listCustomers(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<CustomerResponse> customers = customerService.listCustomers(pageable)
                .map(CustomerResponse::from);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID id) {
        Customer customer = customerService.getCustomer(id);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable UUID id,
                                                           @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @GetMapping("/{id}/loans")
    public ResponseEntity<List<LoanAccountResponse>> getCustomerLoans(@PathVariable UUID id) {
        customerService.getCustomer(id); // Verify customer exists
        List<LoanAccount> loans = loanServicingService.getCustomerLoans(id);
        List<LoanAccountResponse> responses = loans.stream()
                .map(LoanAccountResponse::from).toList();
        return ResponseEntity.ok(responses);
    }
}
