package com.lending.servicing.adapter.in.web;

import com.lending.ledger.domain.model.LedgerEntry;
import com.lending.ledger.port.out.LedgerEntryRepository;
import com.lending.servicing.domain.model.Installment;
import com.lending.servicing.domain.model.LoanAccount;
import com.lending.servicing.domain.model.Repayment;
import com.lending.servicing.domain.service.LoanServicingService;
import com.lending.servicing.domain.service.RepaymentAllocationService;
import com.lending.servicing.port.out.LoanAccountRepository;
import com.lending.servicing.port.out.RepaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanServicingService loanServicingService;
    private final RepaymentAllocationService repaymentAllocationService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentRepository repaymentRepository;

    public LoanController(LoanServicingService loanServicingService,
                          RepaymentAllocationService repaymentAllocationService,
                          LedgerEntryRepository ledgerEntryRepository,
                          LoanAccountRepository loanAccountRepository,
                          RepaymentRepository repaymentRepository) {
        this.loanServicingService = loanServicingService;
        this.repaymentAllocationService = repaymentAllocationService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.repaymentRepository = repaymentRepository;
    }

    @GetMapping
    public ResponseEntity<Page<LoanAccountResponse>> listLoans(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<LoanAccountResponse> loans = loanAccountRepository.findAll(pageable)
                .map(LoanAccountResponse::from);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanAccountResponse> getLoan(@PathVariable UUID id) {
        LoanAccount loanAccount = loanServicingService.getLoanAccount(id);
        return ResponseEntity.ok(LoanAccountResponse.from(loanAccount));
    }

    @PostMapping("/{id}/disburse")
    public ResponseEntity<LoanAccountResponse> disburseLoan(@PathVariable UUID id) {
        LoanAccount loanAccount = loanServicingService.disburseLoan(id);
        return ResponseEntity.ok(LoanAccountResponse.from(loanAccount));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable UUID id) {
        List<Installment> installments = loanServicingService.getSchedule(id);
        List<ScheduleResponse.InstallmentResponse> responses = installments.stream()
                .map(ScheduleResponse.InstallmentResponse::from)
                .toList();
        return ResponseEntity.ok(new ScheduleResponse(id, responses));
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getLedger(@PathVariable UUID id) {
        // Verify loan exists
        loanServicingService.getLoanAccount(id);
        List<LedgerEntry> entries = ledgerEntryRepository.findByLoanAccountId(id);
        List<LedgerEntryResponse> responses = entries.stream()
                .map(LedgerEntryResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{loanId}/repayments")
    public ResponseEntity<RepaymentResponse> recordRepayment(
            @PathVariable UUID loanId,
            @Valid @RequestBody RecordRepaymentRequest request,
            HttpServletRequest httpRequest) {
        String idempotencyKey = httpRequest.getHeader("Idempotency-Key");
        RepaymentAllocationService.RepaymentResult result =
                repaymentAllocationService.recordRepayment(loanId, request.amount(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(RepaymentResponse.from(result));
    }

    @GetMapping("/{loanId}/repayments")
    public ResponseEntity<List<SimpleRepaymentResponse>> listRepayments(@PathVariable UUID loanId) {
        loanServicingService.getLoanAccount(loanId);
        List<Repayment> repayments = repaymentRepository.findByLoanAccountId(loanId);
        List<SimpleRepaymentResponse> responses = repayments.stream()
                .map(r -> new SimpleRepaymentResponse(r.getId(), r.getLoanAccountId(),
                        r.getAmount(), r.getReceivedAt(), r.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    public record SimpleRepaymentResponse(
            UUID id, UUID loanAccountId,
            com.lending.shared.domain.Money amount,
            java.time.Instant receivedAt,
            java.time.Instant createdAt
    ) {}
}
