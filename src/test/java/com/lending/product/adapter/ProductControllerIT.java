package com.lending.product.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String VALID_PRODUCT = """
            {
              "name": "IT Test Product %s",
              "description": "Integration test product",
              "interestRatePerAnnum": "14.00",
              "interestAccrualMethod": "PRE_COMPUTED",
              "minTenureMonths": 3,
              "maxTenureMonths": 12,
              "minPrincipal": {"amount": "10000.0000", "currency": "KES"},
              "maxPrincipal": {"amount": "500000.0000", "currency": "KES"},
              "repaymentFrequency": "MONTHLY",
              "gracePeriodDays": 3,
              "originationFeeChargeMethod": "DEDUCTED_FROM_DISBURSEMENT",
              "allowEarlyRepayment": true,
              "overpaymentPolicy": "REJECT",
              "feeSchedule": []
            }
            """;

    @Test
    void createProduct_returns201() throws Exception {
        String body = String.format(VALID_PRODUCT, UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.interestRatePerAnnum").value(14.00))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createProduct_missingName_returns400() throws Exception {
        String body = """
                {
                  "interestRatePerAnnum": "14.00",
                  "minTenureMonths": 3,
                  "maxTenureMonths": 12,
                  "minPrincipal": {"amount": "10000.0000", "currency": "KES"},
                  "maxPrincipal": {"amount": "500000.0000", "currency": "KES"},
                  "repaymentFrequency": "MONTHLY"
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_idempotencyReplay() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        String body = String.format(VALID_PRODUCT, UUID.randomUUID().toString().substring(0, 8));

        // First request
        MvcResult first = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        // Replay with same key
        MvcResult replay = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(body))
                .andExpect(header().string("X-Idempotent-Replayed", "true"))
                .andReturn();

        // Response body should be identical
        assertEquals(first.getResponse().getContentAsString(),
                replay.getResponse().getContentAsString());
    }

    @Test
    void getProduct_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void mutatingRequest_missingIdempotencyKey_returns400() throws Exception {
        String body = String.format(VALID_PRODUCT, UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected response body to match replay");
        }
    }
}
