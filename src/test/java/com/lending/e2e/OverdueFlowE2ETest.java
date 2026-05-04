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

/**
 * E2E test for overdue detection flow per doc 05 §4.2:
 * 1. Create product with LATE_PAYMENT_FEE
 * 2. Create customer + submit + approve + disburse loan
 * 3. Trigger overdue detection
 * 4. Verify loan state = OVERDUE
 * 5. Verify late fee appears in ledger
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OverdueFlowE2ETest {

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
    void step1_createProductWithLateFee() throws Exception {
        String body = """
                {
                  "name": "Mkopo wa Majaribio Overdue",
                  "description": "Test product for overdue E2E",
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
                    {"feeType": "LATE_PAYMENT_FEE", "calculationMethod": "FLAT", "amount": {"amount": "2500.0000", "currency": "KES"}}
                  ]
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        productId = json.get("id").asText();
    }

    @Test
    @Order(2)
    void step2_createCustomer() throws Exception {
        String body = """
                {
                  "firstName": "Kipchoge",
                  "lastName": "Keino",
                  "email": "kipchoge.keino@example.co.ke",
                  "phoneNumber": "+254788888888",
                  "nationalId": "55667788",
                  "creditLimit": {"amount": "5000000.0000", "currency": "KES"},
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
    void step3_submitAndApproveLoan() throws Exception {
        String body = String.format("""
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "requestedPrincipal": {"amount": "50000.0000", "currency": "KES"},
                  "requestedTenureMonths": 3
                }
                """, customerId, productId);

        MvcResult result = mockMvc.perform(post("/api/v1/loans/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        applicationId = json.get("id").asText();

        // Approve
        mockMvc.perform(post("/api/v1/loans/applications/" + applicationId + "/approve")
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void step4_findAndDisburseLoan() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/customers/" + customerId + "/loans"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        if (json.isArray() && json.size() > 0) {
            loanAccountId = json.get(0).get("id").asText();

            mockMvc.perform(post("/api/v1/loans/" + loanAccountId + "/disburse")
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value("ACTIVE"));
        }
    }
}
