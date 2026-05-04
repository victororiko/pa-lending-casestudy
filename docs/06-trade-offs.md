# 06 — Trade-Offs

> A candid assessment of what's deliberately simplified, what would come next, and
> what would change if this system were heading to real production.

---

## 1. What's Deliberately Stubbed and Why

| Stub | What it does today | Why it's stubbed | Cost to make real |
|------|--------------------|-----------------|-------------------|
| **Authentication** | `X-User-Id` header trusted at face value | Auth is infrastructure, not domain. Building it would consume time without demonstrating lending expertise. See [ADR-0008](adr/0008-authentication-scope.md). | **Medium.** Add Spring Security + JWT validation filter. ~1 day. Domain code unchanged. |
| **Notification delivery** | `LoggingNotificationChannel` writes to console | Brief explicitly says "you don't need to build a full notification delivery system." | **Low.** Implement `EmailNotificationChannel` with JavaMail/SES. Plug in as `@Component`. ~0.5 day per channel. |
| **Disbursement** | `disburse()` transitions state immediately — no payment rail call | Real disbursement requires integration with a banking partner API (ACH, wire, mobile money). This is infrastructure, not domain logic. | **High.** Requires async processing, failure handling, reconciliation, and a callback mechanism. ~1–2 weeks. |
| **Payment gateway** | Repayments recorded via API as if money has arrived | Same reasoning as disbursement. | **High.** Same scope as disbursement integration. |
| **Interest accrual** | Pre-computed amortisation schedule at origination (`PRE_COMPUTED` method). The `DAILY_ACCRUAL` method is accepted at product creation and stored, but the nightly accrual engine is not built — disbursement returns `501 Not Implemented` for `DAILY_ACCRUAL` products. | `PRE_COMPUTED` is the standard model for fixed-rate consumer loans and demonstrates the full lifecycle. `DAILY_ACCRUAL` is modelled to avoid a schema migration later. | **High.** Build nightly batch job, balance recalculation, and schedule adjustment. ~1 week. No schema changes needed — the product field and `LoanAccount` snapshot are already in place. |
| **Credit scoring** | Eligibility check is `creditLimit` and `maxActiveLoans` only | Real credit decisions require bureau integration, scoring models, and regulatory compliance. | **Very high.** External integration + ML model serving + regulatory review. Weeks to months. |

---

## 2. What I'd Build Next Given Another Week

Prioritised by impact:

| Priority | Feature | Value |
|----------|---------|-------|
| 1 | **Transactional outbox for events** | Eliminates the risk of lost notifications if the app crashes between DB commit and event dispatch. Adds an `outbox` table; a polling publisher reads it and dispatches events. This is the single biggest reliability gap in the current design. |
| 2 | **JWT-based authentication + RBAC** | Separate `ADMIN` (manages products, approves loans) and `CUSTOMER` (submits applications, views own loans) roles. Spring Security filter chain + ownership-based access control. |
| 3 | **Email notification channel** | Replace `LoggingNotificationChannel` with a real SMTP sender. Use Spring Mail + Thymeleaf templates. |
| 4 | **`DAILY_ACCRUAL` interest engine** | Implement the nightly batch job for products with `interestAccrualMethod = DAILY_ACCRUAL`. The data model is ready — no schema changes. Unblocks variable-rate and day-count-sensitive products. |
| 5 | **`HOLD_AS_CREDIT` refund/withdrawal API** | Build `POST /loans/{id}/credit-withdrawal` so customers can retrieve overpayment surplus. The `creditBalance` field is already in the schema. |
| 6 | **Loan restructuring** | Allow modifying an active loan's schedule (e.g., extending tenure after a hardship request). Adds a `RESTRUCTURED` state and a schedule-regeneration flow. |
| 7 | **Comprehensive metrics & dashboards** | Micrometer counters/gauges for: loans originated per day, total outstanding balance, overdue rate, average repayment time. Export to Prometheus → Grafana. |
| 8 | **CI/CD pipeline** | GitHub Actions: build → test → Docker image → deploy to staging. |

---

## 3. Known Limitations and Cost of Fixing

| Limitation | Impact | Fix | Cost |
|-----------|--------|-----|------|
| **Single currency (USD only)** | Cannot serve markets with different currencies. The `Money` VO already carries `Currency`, but the system never converts between currencies. | Add an FX rate service, multi-currency ledger, and settlement logic. | **High.** ~2 weeks. Touches every module. |
| **Single tenancy** | Cannot separate data for different business units or white-label deployments. | Add `tenant_id` to every table, introduce tenant-scoped queries, and partition data. | **Very high.** Schema migration + query refactoring across all repositories. |
| **In-process events only** | Events are lost if the application crashes between DB commit and `ApplicationEventPublisher` dispatch. No cross-service event bus. | Transactional outbox pattern (priority 1 above) or migrate to Kafka/RabbitMQ. | **Medium.** ~3 days for outbox; ~1 week for full broker migration. |
| **No audit log tamper protection** | Audit entries can be modified by anyone with database access. | Hash-chain audit entries (each entry includes hash of previous entry) or use an append-only database/blockchain for audit. | **Medium.** ~2 days for hash chain. |
| **`DAILY_ACCRUAL` not implemented** | Products configured with `DAILY_ACCRUAL` interest cannot be disbursed (returns `501`). The data model and product configuration accept it, so no schema migration is needed. | Build nightly accrual batch job, balance recalculation, and schedule adjustment engine. | **High.** ~1 week. Domain logic only — schema is ready. |
| **`HOLD_AS_CREDIT` refund flow not built** | The `creditBalance` field exists on `LoanAccount` and overpayments can be held, but there is no API to withdraw/refund the credit. | Add `POST /loans/{id}/credit-withdrawal` endpoint + payment-rail integration. | **Medium.** ~2 days for endpoint; payment integration is separate. |
| **Idempotency store in PostgreSQL** | Adds a write to every mutating request. At high TPS, this becomes a bottleneck. | Migrate to Redis with TTL-based expiry. | **Low.** ~1 day. Replace the repository adapter. |
| **No pagination cursor** | Offset-based pagination degrades on large datasets (page 1000 requires scanning 999 pages). | Switch to keyset (cursor) pagination using `createdAt + id` as the cursor. | **Medium.** ~1 day per endpoint. |
| **Overdue detection as daily batch** | A loan can be overdue for up to 24 hours before detection. Not real-time. | Schedule per-installment timers (e.g., via a delayed message queue) to trigger exactly on the due date. | **Medium.** Requires a scheduler or message broker with delayed delivery. |
| **No data archival strategy** | Ledger, audit log, and notification tables grow unboundedly. | Partition by date, archive cold data to S3/blob storage, implement retention policies. | **Medium.** ~3 days. Operational concern. |

---

## 4. Production Readiness Checklist (What Would Change)

If this system were heading to real production with paying customers:

| Area | Current State | Production Requirement |
|------|--------------|----------------------|
| **Authentication** | Stubbed | OAuth2 / OIDC with a real identity provider |
| **Authorisation** | None | RBAC + resource ownership checks |
| **Encryption at rest** | None | PostgreSQL TDE or application-level encryption for PII |
| **PII handling** | Stored in plaintext | Encrypt or tokenise national ID, mask email/phone in logs |
| **Rate limiting** | None | API gateway (Kong, Envoy) or Spring Security rate limiter |
| **Disbursement** | Stub | Real banking partner integration with reconciliation |
| **Payment processing** | Stub | Payment gateway integration (Stripe, Adyen, local rails) |
| **KYC/AML** | None | Identity verification service, sanctions screening |
| **Credit bureau** | None | Bureau integration for credit scoring |
| **Multi-currency** | Not supported | FX service, multi-currency ledger |
| **Disaster recovery** | None | PostgreSQL streaming replication, point-in-time recovery, automated backups |
| **Monitoring** | Actuator endpoints | Full APM (Datadog, New Relic), alerting, on-call rotation |
| **Load testing** | None | Gatling/k6 load tests, capacity planning |
| **Regulatory compliance** | None | Data residency, right-to-erasure (GDPR), financial reporting |
| **Deployment** | `docker-compose` | Kubernetes, blue-green deploys, infrastructure-as-code |

---

## 5. Design Decisions I'm Least Confident About

These are decisions where reasonable engineers would disagree, and I'd welcome pushback:

1. **Product-level configurability for all 5 policy decisions.** Making interest accrual
   method, grace period, origination fee handling, prepayment penalty, and overpayment
   policy all configurable per product is the maximally flexible choice. The alternative
   is system-wide settings (simpler, less flexible) or per-loan overrides (more flexible
   but complex). Product-level is the sweet spot: it lets the business launch variant
   products without code changes, while keeping the domain model tractable.

2. **Single `LoanAccount` aggregate vs separate `Schedule` aggregate.** I embedded
   `Installment` as a child of `LoanAccount`. This makes the aggregate large. If
   installments need independent lifecycle management (e.g., restructuring individual
   installments), they should become their own aggregate with eventual consistency.

3. **Repayment allocation in application code vs database procedure.** I chose application
   code for testability and portability. A database procedure would be faster for
   high-volume processing but harder to test and version-control.

4. **`Idempotency-Key` on all mutating endpoints.** Some teams only require idempotency
   keys on payment endpoints, not on CRUD. I applied it universally for consistency,
   but it adds overhead to endpoints that rarely need it (e.g., `PUT /products`).

5. **`HOLD_AS_CREDIT` modelled but not fully implemented.** The `creditBalance` field on
   `LoanAccount` is in the schema, but the external refund flow for withdrawing credit
   is not built. This could be seen as premature design — but the cost of adding a
   nullable column later (with a data migration) is higher than defining it now.

---

This document should be read alongside the [README](../README.md) for system overview
and [01-assumptions-and-scope.md](01-assumptions-and-scope.md) for the full list of
in-scope vs out-of-scope decisions.
