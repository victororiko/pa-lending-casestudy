package com.lending.origination.adapter.in.web;

import com.lending.origination.domain.model.LoanApplication;
import com.lending.origination.domain.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans/applications")
public class ApplicationController {

    private final LoanApplicationService applicationService;

    public ApplicationController(LoanApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> submitApplication(
            @Valid @RequestBody SubmitApplicationRequest request) {
        LoanApplication app = applicationService.submitApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(app));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable UUID id) {
        LoanApplication app = applicationService.getApplication(id);
        return ResponseEntity.ok(ApplicationResponse.from(app));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApplicationResponse> approveApplication(@PathVariable UUID id) {
        LoanApplication app = applicationService.approveApplication(id);
        return ResponseEntity.ok(ApplicationResponse.from(app));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApplicationResponse> rejectApplication(@PathVariable UUID id,
                                                                  @RequestBody RejectApplicationRequest request) {
        LoanApplication app = applicationService.rejectApplication(id, request.reason());
        return ResponseEntity.ok(ApplicationResponse.from(app));
    }
}
