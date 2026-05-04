package com.lending.shared.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String REPLAYED_HEADER = "X-Idempotent-Replayed";
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH");

    private final IdempotencyStore idempotencyStore;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final long ttlHours;

    public IdempotencyFilter(IdempotencyStore idempotencyStore,
                             PlatformTransactionManager transactionManager,
                             Clock clock,
                             @Value("${app.idempotency.ttl-hours:24}") long ttlHours) {
        this.idempotencyStore = idempotencyStore;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.ttlHours = ttlHours;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();

        if (!MUTATING_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":400,\"error\":\"Bad Request\",\"code\":\"MISSING_IDEMPOTENCY_KEY\"," +
                            "\"message\":\"Idempotency-Key header is required for mutating requests.\"}");
            return;
        }

        // Check for existing record
        Optional<IdempotencyRecord> existing = transactionTemplate.execute(status ->
                idempotencyStore.findByKeyForUpdate(idempotencyKey));

        if (existing != null && existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            if (record.getExpiresAt().isBefore(Instant.now(clock))) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":409,\"error\":\"Conflict\",\"code\":\"IDEMPOTENCY_KEY_EXPIRED\"," +
                                "\"message\":\"The idempotency key has expired. Please use a new key.\"}");
                return;
            }

            // Replay the stored response
            response.setStatus(record.getResponseStatus());
            response.setContentType("application/json");
            response.setHeader(REPLAYED_HEADER, "true");
            if (record.getResponseBody() != null) {
                response.getWriter().write(record.getResponseBody());
            }
            log.info("Idempotent replay for key: {}", idempotencyKey);
            return;
        }

        // Execute the request and capture response
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        // Store the response
        String responseBody = new String(responseWrapper.getContentAsByteArray(),
                responseWrapper.getCharacterEncoding());
        int responseStatus = responseWrapper.getStatus();

        try {
            Instant now = Instant.now(clock);
            IdempotencyRecord record = new IdempotencyRecord(
                    idempotencyKey,
                    method,
                    request.getRequestURI(),
                    responseStatus,
                    responseBody,
                    now,
                    now.plusSeconds(ttlHours * 3600)
            );
            transactionTemplate.executeWithoutResult(status -> idempotencyStore.save(record));
        } catch (Exception e) {
            log.warn("Failed to store idempotency record for key {}: {}", idempotencyKey, e.getMessage());
        }

        responseWrapper.copyBodyToResponse();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
    }
}
