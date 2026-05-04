# ADR-0006: Idempotency Strategy

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

Financial APIs must be safe to retry. Network failures, client timeouts, and load
balancer retries can cause the same request to arrive multiple times. Without idempotency
guarantees:

- A repayment could be applied twice, over-crediting the customer.
- A loan could be disbursed twice, doubling the exposure.
- A customer could be created with duplicate records.

The brief emphasises "edge cases" and "resilience." Idempotency is the foundational
mechanism for both.

## Decision

All mutating endpoints (`POST`, `PUT`, `PATCH`) require an **`Idempotency-Key` header**
(client-generated UUID). The server guarantees at-most-once execution per key.

### Mechanism

1. **Request arrives** with `Idempotency-Key: <uuid>`.
2. **Lookup** the key in the `idempotency_store` table.
3. **If not found:** execute the operation, store the key + response (status code + body)
   in the table within the same database transaction, return the response.
4. **If found and not expired:** return the stored response (same status code + body)
   without re-executing the operation. Add `X-Idempotent-Replayed: true` header.
5. **If found and expired:** return `409 Conflict` — the client must generate a new key.
6. **If missing header on a mutating endpoint:** return `400 Bad Request`.

### Storage

```sql
CREATE TABLE idempotency_store (
    idempotency_key  VARCHAR(64)  PRIMARY KEY,
    http_method      VARCHAR(10)  NOT NULL,
    request_path     VARCHAR(512) NOT NULL,
    response_status  INT          NOT NULL,
    response_body    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_store (expires_at);
```

### TTL and Cleanup

- **TTL: 24 hours.** After 24 hours, the key expires and is eligible for cleanup.
  Rationale: 24 hours covers overnight retries, batch reprocessing, and human-driven
  "click again" scenarios. Shorter TTLs risk legitimate retries being treated as new
  requests.
- **Cleanup:** A scheduled job (`@Scheduled`, daily) deletes rows where
  `expires_at < NOW()`. This keeps the table small.

### Implementation: Servlet Filter

Idempotency is implemented as a **Spring `OncePerRequestFilter`** that wraps the
entire request/response cycle:

1. Intercepts all mutating requests.
2. Checks the idempotency store.
3. If replay: short-circuits to stored response.
4. If new: wraps the response in a `ContentCachingResponseWrapper`, executes the
   chain, captures the response, stores it, and returns it.

This approach is orthogonal to business logic — no service or controller knows about
idempotency.

### Concurrency

Two identical requests arriving simultaneously are handled via a `SELECT ... FOR UPDATE`
on the idempotency key. The first request acquires the lock, executes, and stores the
result. The second request waits on the lock, then reads the stored result.

## Consequences

### Positive

- Retry-safe by default for every mutating endpoint. No per-endpoint opt-in needed.
- Stored in the same database, same transaction as the business operation — no
  distributed coordination.
- Filter-based implementation means zero business logic changes.
- Client can safely retry any failed request with the same key.

### Negative

- Adds a database write to every mutating request (the idempotency record). For a
  case study with low throughput, this is negligible. At scale, the store could be
  moved to Redis for lower latency.
- 24-hour TTL means the table can grow. Mitigated by the cleanup job.
- `response_body` column stores the full response, which could be large for list
  endpoints. Mitigated by only applying idempotency to mutating endpoints (which
  typically return single-entity responses).

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **Client-side deduplication only** | Unreliable. The server cannot trust the client to deduplicate, and network-level retries (load balancers, proxies) are invisible to the client. |
| **Redis-based idempotency store** | Lower latency at scale, but adds an infrastructure dependency. PostgreSQL is sufficient for case-study throughput and keeps the stack simple. Would migrate to Redis if the system hits thousands of TPS. |
| **Database unique constraint on business key** (e.g., unique repayment per loan + date) | Covers some cases but not all. A unique constraint on `(loan_id, idempotency_key)` is what we're building — the `idempotency_store` generalises this to all endpoints. |
| **No idempotency — rely on client retries being rare** | Unacceptable for financial operations. Even a 0.1% retry rate on repayments could cause real monetary loss. |
| **ETag-based conditional requests** | Suitable for `PUT`/`PATCH` (optimistic concurrency), but does not cover `POST` (creating new resources). `Idempotency-Key` is more general. |
