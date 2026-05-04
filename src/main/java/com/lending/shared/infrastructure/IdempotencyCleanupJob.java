package com.lending.shared.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyStore idempotencyStore;
    private final Clock clock;

    public IdempotencyCleanupJob(IdempotencyStore idempotencyStore, Clock clock) {
        this.idempotencyStore = idempotencyStore;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM UTC
    @Transactional
    public void cleanupExpiredKeys() {
        int deleted = idempotencyStore.deleteExpired(Instant.now(clock));
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency keys", deleted);
        }
    }
}
