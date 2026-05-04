package com.lending.customer.domain.model;

import com.lending.shared.domain.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Customer {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String nationalId;
    private Money creditLimit;
    private int maxActiveLoans;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public Customer() {
        this.id = UUID.randomUUID();
        this.maxActiveLoans = 3;
    }

    public void validate() {
        Objects.requireNonNull(firstName, "firstName must not be null");
        if (firstName.isBlank()) throw new IllegalArgumentException("firstName must not be blank");
        if (firstName.length() > 100) throw new IllegalArgumentException("firstName must not exceed 100 characters");

        Objects.requireNonNull(lastName, "lastName must not be null");
        if (lastName.isBlank()) throw new IllegalArgumentException("lastName must not be blank");

        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        Objects.requireNonNull(nationalId, "nationalId must not be null");

        Objects.requireNonNull(creditLimit, "creditLimit must not be null");
        if (creditLimit.isNegative()) throw new IllegalArgumentException("creditLimit must be >= 0");

        if (maxActiveLoans < 1) throw new IllegalArgumentException("maxActiveLoans must be >= 1");
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public Money getCreditLimit() { return creditLimit; }
    public void setCreditLimit(Money creditLimit) { this.creditLimit = creditLimit; }

    public int getMaxActiveLoans() { return maxActiveLoans; }
    public void setMaxActiveLoans(int maxActiveLoans) { this.maxActiveLoans = maxActiveLoans; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
