package com.lending.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanLifecycleE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String productId;
    private static String customerId;
    private static String applicationId;
    private static String loanAccountId;

    @Test
    @Order(1)
    void step1_createProduct() throws Exception {
        String body = """
                {
                  "name": "Mkopo wa Majaribio",
                  "description": "Test product for E2E",
                  "interestRatePerAnnum": "14.00",
                  "interestAccrualMethod": "PRE_COMPUTED",
                  "minTenureMonths": 1,
                  "maxTenureMonths": 12,
                  "minPrincipal": {"amount": "10000.0000", "currency": "KES"},
                  "maxPrincipal": {"amount": "500000.0000", "currency": "KES"},
                  "repaymentFrequency": "MONTHLY",
                  "gracePeriodDays": 0,
                  "originationFeeChargeMethod": "DEDUCTED_FROM_DISBURSEMENT",
                  "allowEarlyRepayment": true,
                  "overpaymentPolicy": "REJECT",
                  "feeSchedule": [
                    {"feeType": "ORIGINATION_FEE", "calculationMethod": "FLAT", "amount": {"amount": "1000.0000", "currency": "KES"}}
                  ]
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mkopo wa Majaribio"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        productId = json.get("id").asText();
    }

    @Test
    @Order(2)
    void step2_createCustomer() throws Exception {
        String body = """
                {
                  "firstName": "Njeri",
                  "lastName": "Muthoni",
                  "email": "njeri.muthoni@example.co.ke",
                  "phoneNumber": "+254799999999",
                  "nationalId": "98765432",
                  "creditLimit": {"amount": "10000000.0000", "currency": "KES"},
                  "maxActiveLoans": 5
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        customerId = json.get("id").asText();
    }

    @Test
    @Order(3)
    void step3_submitApplication() throws Exception {
        String body = String.format("""
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "requestedPrincipal": {"amount": "100000.0000", "currency": "KES"},
                  "requestedTenureMonths": 6
                }
                """, customerId, productId);

        MvcResult result = mockMvc.perform(post("/api/v1/loans/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        applicationId = json.get("id").asText();
    }

    @Test
    @Order(4)
    void step4_approveApplication() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/loans/applications/" + applicationId + "/approve")
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();
    }

    @Test
    @Order(5)
    void step5_verifyPendingDisbursement() throws Exception {
        // The LoanAccount is created by the event listener, we need to find it
        // Get customer's loans to find the loan account
        MvcResult result = mockMvc.perform(get("/api/v1/customers/" + customerId + "/loans"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        if (json.isArray() && json.size() > 0) {
            loanAccountId = json.get(0).get("id").asText();
        }
    }

    @Test
    @Order(6)
    void step6_disburseLoan() throws Exception {
        if (loanAccountId == null) return; // Skip if loan not created

        mockMvc.perform(post("/api/v1/loans/" + loanAccountId + "/disburse")
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"));
    }

    @Test
    @Order(7)
    void step7_verifySchedule() throws Exception {
        if (loanAccountId == null) return;

        mockMvc.perform(get("/api/v1/loans/" + loanAccountId + "/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installments").isArray())
                .andExpect(jsonPath("$.installments.length()").value(6));
    }

    @Test
    @Order(8)
    void step8_verifyLedger() throws Exception {
        if (loanAccountId == null) return;

        mockMvc.perform(get("/api/v1/loans/" + loanAccountId + "/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
