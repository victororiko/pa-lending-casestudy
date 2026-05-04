package com.lending.servicing.domain.model;

import com.lending.shared.domain.Money;

import java.time.LocalDate;
import java.util.UUID;

public class Installment {

    private UUID id;
    private UUID loanAccountId;
    private int installmentNumber;
    private LocalDate dueDate;
    private Money principalDue;
    private Money interestDue;
    private Money totalDue;
    private Money principalPaid;
    private Money interestPaid;
    private Money totalPaid;
    private String currency;
    private InstallmentStatus status;

    public Installment() {
        this.id = UUID.randomUUID();
        this.status = InstallmentStatus.PENDING;
    }

    public Money remainingTotal() {
        return totalDue.subtract(totalPaid);
    }

    public Money remainingInterest() {
        return interestDue.subtract(interestPaid);
    }

    public Money remainingPrincipal() {
        return principalDue.subtract(principalPaid);
    }

    public boolean isFullyPaid() {
        return totalPaid.isGreaterThanOrEqual(totalDue);
    }

    public void applyPayment(Money interestPayment, Money principalPayment) {
        this.interestPaid = this.interestPaid.add(interestPayment);
        this.principalPaid = this.principalPaid.add(principalPayment);
        this.totalPaid = this.interestPaid.add(this.principalPaid);

        if (isFullyPaid()) {
            this.status = InstallmentStatus.PAID;
        } else if (this.totalPaid.isPositive()) {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLoanAccountId() { return loanAccountId; }
    public void setLoanAccountId(UUID loanAccountId) { this.loanAccountId = loanAccountId; }

    public int getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(int installmentNumber) { this.installmentNumber = installmentNumber; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Money getPrincipalDue() { return principalDue; }
    public void setPrincipalDue(Money principalDue) { this.principalDue = principalDue; }

    public Money getInterestDue() { return interestDue; }
    public void setInterestDue(Money interestDue) { this.interestDue = interestDue; }

    public Money getTotalDue() { return totalDue; }
    public void setTotalDue(Money totalDue) { this.totalDue = totalDue; }

    public Money getPrincipalPaid() { return principalPaid; }
    public void setPrincipalPaid(Money principalPaid) { this.principalPaid = principalPaid; }

    public Money getInterestPaid() { return interestPaid; }
    public void setInterestPaid(Money interestPaid) { this.interestPaid = interestPaid; }

    public Money getTotalPaid() { return totalPaid; }
    public void setTotalPaid(Money totalPaid) { this.totalPaid = totalPaid; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public InstallmentStatus getStatus() { return status; }
    public void setStatus(InstallmentStatus status) { this.status = status; }
}
