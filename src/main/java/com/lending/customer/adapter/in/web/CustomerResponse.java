package com.lending.customer.adapter.in.web;

import com.lending.customer.domain.model.Customer;
import com.lending.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String nationalId,
        Money creditLimit,
        int maxActiveLoans,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getNationalId(),
                customer.getCreditLimit(),
                customer.getMaxActiveLoans(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getVersion()
        );
    }
}
