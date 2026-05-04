# 01 — Assumptions and Scope

> This document is the contract between the case-study brief and the implementation.
> Every implicit business rule is surfaced, every scope boundary is explicit, and domain
> vocabulary is locked down before a single line of code is written.

---

## 1. In-Scope Features (fully built)

| Area | Feature | Notes |
|------|---------|-------|
| **Product Catalog** | CRUD for loan products with configurable parameters (interest rate, tenure range, fee schedule, repayment frequency) | Admin-facing; no UI, API only |
| **Customer Management** | Create / update customer profiles, enforce per-customer lending limits, track active loans | Minimal KYC — name, email, phone, national ID stub |
| **Loan Origination** | Apply for a loan, validate against product rules & customer limits, approve/reject, disburse | Happy path + common rejection paths |
| **Loan Servicing** | Record repayments (full, partial, overpayment), update amortisation schedule, transition loan state | Includes interest + principal allocation |
| **Overdue Detection** | Scheduled job to flag loans with missed installments and transition them to `OVERDUE` | Cron-triggered, idempotent |
| **Notifications (stub)** | Emit domain events at lifecycle transitions; log-based notification channel that records what *would* be sent | Demonstrates extensibility without real delivery |
| **Ledger Entries** | Append-only ledger recording every monetary movement (disbursement, repayment, fee, write-off) | Double-entry bookkeeping not in scope; single-entry journal |
| **Idempotency** | All mutating endpoints accept an `Idempotency-Key` header; replays return the original response | Server-side key store with TTL |
| **Audit Columns** | Every entity carries `created_at`, `updated_at`, `created_by`, `version` | Optimistic locking via `version` |
| **API Documentation** | Auto-generated OpenAPI 3.1 spec via springdoc-openapi | Available at `/v3/api-docs` and `/swagger-ui.html` |

## 2. Out-of-Scope / Deliberately Stubbed

| Item | Status | Rationale |
|------|--------|-----------|
| **Authentication & Authorisation** | Stubbed — all endpoints open; `created_by` populated from a static header `X-User-Id` | The brief does not mention auth; building a real IAM layer would consume disproportionate time without demonstrating lending domain skill. See [ADR-0008](adr/0008-authentication-scope.md). |
| **Real notification delivery** (email, SMS, push) | Stubbed — `LoggingNotificationChannel` writes to application log | The brief explicitly says "you don't need to build a full notification delivery system." |
| **Multi-currency** | Not supported — system assumes a single operating currency (`USD`) configured at startup | Multi-currency adds FX rate management, settlement complexity, and regulatory concerns that dwarf the case study scope. |
| **Multi-tenancy** | Not supported | Single-tenant deployment assumed. |
| **Interest accrual engine** | Simplified — interest is pre-computed at origination into a flat amortisation schedule | Real production systems accrue daily on outstanding balance. Pre-computed schedules are sufficient to demonstrate lifecycle management. |
| **Credit scoring / KYC** | Not built — customer eligibility is enforced only via a configurable `max_active_loans` and `credit_limit` on the customer entity | Real lending requires bureau integration; this is out of scope. |
| **Disbursement integration** | Stubbed — disbursement transitions the loan to `ACTIVE` immediately without calling an external payment rail | Demonstrates state transition; real integration is infrastructure work. |
| **Payment gateway** | Stubbed — repayments are recorded via API call as if the money has already arrived | Same reasoning as disbursement. |
| **UI / Frontend** | None | Brief asks for backend only. |
| **Containerisation / CI-CD** | `docker-compose.yml` for PostgreSQL provided; no Kubernetes, no pipeline definitions | Sufficient for "run from scratch on any machine." |
| **Rate limiting / Throttling** | Not built | Would be handled at API gateway layer in production. |
| **Soft deletes** | Products and customers use soft delete (`deleted_at`); loans and ledger entries are never deleted | Financial records must be immutable. |

## 3. Implicit Business Rules — Stated as Assumptions

Each assumption is derived from gaps in the brief. If the business disagrees, the
assumption should be updated here _before_ implementation proceeds.

| # | Assumption | Justification |
|---|-----------|---------------|
| A1 | **Interest accrual method is configurable per product** via `interestAccrualMethod` (`PRE_COMPUTED` or `DAILY_ACCRUAL`). `PRE_COMPUTED` generates a fixed amortisation schedule at origination (EMI). `DAILY_ACCRUAL` computes interest nightly on the outstanding balance. **v1 implements `PRE_COMPUTED` only;** the `DAILY_ACCRUAL` path is defined in the domain model and validated at product creation, but the accrual engine is stubbed. This keeps the architecture ready for variable-rate and day-count-sensitive products. | The brief says "repayments over time" and mentions installment loans. Making the model configurable per product avoids a structural rewrite when the business introduces new product types. |
| A2 | **Grace period is configurable per product** via `gracePeriodDays` (integer ≥ 0, default 0). A loan becomes `OVERDUE` only if an installment remains unpaid after `dueDate + gracePeriodDays`. The overdue detection job compares against `dueDate + gracePeriodDays`, not `dueDate` alone. A value of 0 means strict due-date enforcement (safest default). | The brief mentions "loans that go overdue" but does not define when. Making this configurable lets the business offer lenient consumer products and strict commercial products from the same platform. |
| A3 | **Overdue detection runs as a scheduled background job (daily).** It does not trigger in real-time on the due date. | Batch detection is operationally simpler and idempotent. Real-time detection would require a scheduler per installment, which is over-engineering for this scope. |
| A4 | **A customer's lending limit (`credit_limit`) is a hard cap on total outstanding principal across all active loans.** A new loan application is rejected if `current_outstanding + requested_principal > credit_limit`. | The brief says "limits on how much they can borrow." This is the most straightforward enforcement. |
| A5 | **A customer may not have more than `max_active_loans` loans in non-terminal states simultaneously.** Default: 3. | Prevents runaway exposure. Configurable per customer. |
| A6 | **Partial repayments are applied to the earliest unpaid installment first (FIFO), interest before principal.** | Standard lending convention. Protects the lender's interest income. |
| A7 | **Overpayment policy is configurable per product** via `overpaymentPolicy` enum: `REJECT` (return error — client must submit exact payoff), `APPLY_TO_NEXT_INSTALLMENT` (surplus is allocated to the next unpaid installment, reducing future obligations), or `HOLD_AS_CREDIT` (surplus is held as a credit balance on the `LoanAccount`, applicable to future installments or refundable externally). Default: `REJECT`. **v1 fully implements `REJECT` and `APPLY_TO_NEXT_INSTALLMENT`;** `HOLD_AS_CREDIT` is modelled (field on `LoanAccount`) but the external refund flow is stubbed. | The brief mentions flexibility. Different products have different customer expectations — payday loans reject overpayments; instalment loans often allow them. Configurability avoids hardcoding a single policy. |
| A8 | **Early repayment (payoff) is configurable per product** via `allowEarlyRepayment` (boolean, default `true`) and an optional `PREPAYMENT_PENALTY` in the `feeSchedule`. If `allowEarlyRepayment` is `false`, payoff requests before the final installment are rejected. If a `PREPAYMENT_PENALTY` fee is defined (either `FLAT` or `PERCENTAGE_OF_OUTSTANDING`), the penalty is computed and included in the payoff amount. The penalty ledger entry is recorded alongside the repayment. Default: early repayment allowed, no penalty. | The brief does not mention penalties, but a configurable fee schedule that includes optional prepayment penalties is trivial to add and makes the product model significantly more flexible. Hardcoding "no penalty" would require a schema change later. |
| A9 | **Fees are defined at the product level and attached to the loan at origination.** Three fee types are modelled: `ORIGINATION_FEE` (flat or percentage of principal — how it is charged is controlled by `originationFeeChargeMethod`), `LATE_PAYMENT_FEE` (flat amount, applied per overdue installment by the overdue job), and `PREPAYMENT_PENALTY` (flat or percentage of outstanding balance, applied on early payoff). Origination fee charge method is configurable per product via `originationFeeChargeMethod` enum: `DEDUCTED_FROM_DISBURSEMENT` (customer receives `principal - fee`) or `ADDED_TO_BALANCE` (customer receives full `principal`, fee is added to outstanding balance). Default: `DEDUCTED_FROM_DISBURSEMENT`. | The brief mentions "what it costs." Three fee types cover origination, servicing, and early exit. Making the origination fee charge method configurable is critical — different markets have different conventions, and this is a product-level decision, not a system-level one. |
| A10 | **All monetary values are stored as `NUMERIC(19,4)` in PostgreSQL and represented as `BigDecimal` wrapped in a `Money` value object in Java.** | See [ADR-0007](adr/0007-money-representation.md). Financial precision is non-negotiable. |
| A11 | **Currency is fixed to `USD` for this case study.** The `Money` value object carries a `Currency` field to make multi-currency a future extension, but the system does not perform FX conversion. | Simplifies scope while keeping the door open. |
| A12 | **Loan products are versioned via soft-copy.** When a product is updated, existing loans retain the parameters they were originated with (snapshotted onto the `LoanAccount`). New applications use the latest product version. | Changing a product's interest rate must not retroactively alter existing loans. |
| A13 | **The amortisation schedule uses equal installments (EMI — Equated Monthly Installment) for monthly products.** Weekly or bi-weekly frequencies are supported but the calculation method is the same (annuity formula). | The brief mentions "installment loans." EMI is the industry standard for consumer lending. |
| A14 | **All timestamps are stored in UTC.** Display timezone conversion is a client concern. | Standard practice for backend systems. |
| A15 | **The API is RESTful with JSON payloads.** No GraphQL, no gRPC. | REST is the most universally understood API style and sufficient for this domain. |
| A16 | **Product-level configuration is the extensibility lever.** All five business-policy decisions (interest accrual method, grace period, origination fee handling, prepayment penalty, overpayment policy) are configurable per `LoanProduct`, not system-wide settings. This means two products can coexist with different policies. | Configurability at the product level is the single highest-leverage design decision for a lending platform. It allows the business to launch new product variants without code changes. |
| A17 | **Configuration is snapshotted onto `LoanAccount` at origination.** If a product's grace period changes from 3 days to 0, existing loans retain their original 3-day grace period. | Same soft-copy principle as A12, extended to all new configurable fields. |
| A18 | **v1 implements `PRE_COMPUTED` interest only.** `DAILY_ACCRUAL` is accepted at the product-creation API and stored, but the nightly accrual job is not built. Attempting to disburse a loan for a `DAILY_ACCRUAL` product returns `501 Not Implemented`. | Signals intent and locks the data model without over-building. |
| A19 | **v1 implements `REJECT` and `APPLY_TO_NEXT_INSTALLMENT` overpayment policies.** `HOLD_AS_CREDIT` is modelled (credit balance field on `LoanAccount`) but the external refund flow is not built. | Credit-hold is architecturally simple (a field) but operationally complex (refund integration). Modelling it now prevents a schema migration later. |
| A20 | **`PREPAYMENT_PENALTY` fee uses a new calculation method `PERCENTAGE_OF_OUTSTANDING`** in addition to `FLAT`. This is a third value in the `FeeCalculationMethod` enum. | Prepayment penalties in the industry are typically a percentage of remaining balance, not of original principal. A new enum value is cleaner than overloading `PERCENTAGE_OF_PRINCIPAL`. |

## 4. Non-Functional Requirements

| NFR | Target | Mechanism |
|-----|--------|-----------|
| **Idempotency** | All `POST` / `PUT` / `PATCH` endpoints are safe to retry | `Idempotency-Key` header → server-side cache with 24 h TTL. See [ADR-0006](adr/0006-idempotency-strategy.md). |
| **Auditability** | Every state change and monetary movement is traceable | Append-only `ledger_entries` table + `audit_log` table capturing entity snapshots. See [ADR-0009](adr/0009-audit-logging-strategy.md). |
| **Extensibility** | New loan product types, notification channels, and fee types can be added without modifying core domain logic | Hexagonal architecture + strategy pattern for fees, event-driven notifications. See [ADR-0001](adr/0001-hexagonal-architecture.md). |
| **Observability hooks** | Structured logging, health endpoint, metrics endpoint | Spring Boot Actuator (`/actuator/health`, `/actuator/metrics`). Full APM integration is out of scope but hooks are in place. |
| **Data integrity** | No money is lost, no state is corrupted | Database-level constraints, optimistic locking (`version` column), transactional boundaries at the use-case level. |
| **Testability** | Domain logic is testable without Spring context or database | Hexagonal architecture isolates domain from infrastructure. See [ADR-0010](adr/0010-testing-strategy.md). |

## 5. Glossary of Domain Terms

Consistent vocabulary is enforced across all documentation and code. If a term is not in
this glossary, it must be added here before use.

| Term | Definition | Not to be confused with |
|------|-----------|------------------------|
| **LoanProduct** | A template defining the rules under which money can be lent: interest rate, tenure range, fee schedule, repayment frequency, maximum principal. | Not a `LoanAccount`. A `LoanProduct` is never "disbursed." |
| **LoanAccount** | A specific instance of lending: one customer borrowed X amount under the rules of one `LoanProduct`. Carries its own state, schedule, and ledger. | Not `Loan` (ambiguous) and not `LoanApplication` (which is the request phase only). |
| **LoanApplication** | The request a customer submits to borrow money. It references a `LoanProduct` and a `Customer`. It transitions to `APPROVED` or `REJECTED`. Once approved and disbursed, a `LoanAccount` is created. | Not a `LoanAccount` — the application is the *request*; the account is the *obligation*. |
| **Customer** | A person or entity that borrows money. Holds profile data, a `credit_limit`, and `max_active_loans`. | Not `User` (which implies authentication) and not `Borrower` (informal). |
| **Principal** | The original amount disbursed to the customer, excluding fees and interest. | Not `Outstanding Balance` (which includes accrued interest and fees). |
| **Outstanding Balance** | The total amount the customer still owes: remaining principal + accrued interest + unpaid fees. | Not `Principal`. |
| **Installment** | A single scheduled payment within the amortisation schedule. Contains a due date, principal portion, interest portion, and payment status. | Not `Repayment` (which is the actual payment event). |
| **Repayment** | An actual payment made by the customer, recorded against one or more installments. | Not `Installment`. |
| **Tenure** (syn. **Term**) | The total duration of the loan, expressed as a number of periods (e.g., 12 months). | Both words are acceptable; `tenure` is preferred in code. `Term` is acceptable in user-facing text. |
| **Fee** | A charge applied to a loan, either at origination (`ORIGINATION_FEE`) or during servicing (`LATE_PAYMENT_FEE`). | Not `Interest`. Fees are discrete charges; interest is a time-based cost of capital. |
| **Interest** | The cost of borrowing, expressed as an annual percentage rate (APR) and broken down into per-period amounts in the amortisation schedule. | Not `Fee`. |
| **Interest Rate** | The annual percentage rate (APR) defined on the `LoanProduct` and snapshotted onto the `LoanAccount`. | Always APR; the system converts to per-period rate internally. |
| **Amortisation Schedule** | The complete set of `Installments` generated at loan approval, defining when and how much the customer pays. | Immutable once generated (recalculated only on early payoff or restructuring). |
| **Ledger Entry** | An append-only record of a monetary event: disbursement, repayment allocation, fee charge, or write-off. | Not an `Installment` and not a `Repayment`. A single repayment may generate multiple ledger entries. |
| **Loan State** | The current phase of a `LoanAccount`'s lifecycle: `PENDING_APPROVAL` → `APPROVED` → `ACTIVE` → `CLOSED` (or `OVERDUE`, `DEFAULTED`, `WRITTEN_OFF`). | See the state machine in [doc 02](02-domain-model.md) and [ADR-0004](adr/0004-state-machine-for-loan-lifecycle.md). |
| **Interest Accrual Method** | A product-level setting controlling how interest is computed: `PRE_COMPUTED` (fixed EMI schedule at origination) or `DAILY_ACCRUAL` (nightly calculation on outstanding balance). | Not to be confused with `Interest Rate`, which is the APR input to either method. |
| **Grace Period** | The number of days after an installment's `dueDate` before the loan is considered `OVERDUE`. Configurable per product via `gracePeriodDays`. | Not a "deferment" (which suspends the entire schedule). Grace period only delays the overdue classification. |
| **Origination Fee Charge Method** | How the origination fee is applied: `DEDUCTED_FROM_DISBURSEMENT` (customer receives `principal - fee`) or `ADDED_TO_BALANCE` (customer receives full principal, fee increases outstanding balance). Configurable per product. | Not to be confused with `FeeCalculationMethod` (which determines the fee *amount*: flat vs percentage). |
| **Overpayment Policy** | A product-level setting controlling what happens when a repayment exceeds the outstanding balance: `REJECT`, `APPLY_TO_NEXT_INSTALLMENT`, or `HOLD_AS_CREDIT`. | Not a "refund" — refunds are external payment-rail operations. |
| **Prepayment Penalty** | A fee charged when a borrower pays off a loan before its scheduled maturity. Configurable per product via the `PREPAYMENT_PENALTY` fee type. | Not an `ORIGINATION_FEE` (charged at origination) or `LATE_PAYMENT_FEE` (charged on overdue). |
| **Credit Balance** | A positive monetary amount held on a `LoanAccount` when an overpayment exceeds the outstanding balance under `HOLD_AS_CREDIT` policy. Applied automatically to future installments or refunded externally. | Not `Outstanding Balance` (which is money owed). Credit balance is money the lender owes the customer. |
| **Idempotency Key** | A client-generated unique identifier sent in the `Idempotency-Key` HTTP header to ensure a mutating operation is applied at most once. | Not a request ID (which is for tracing). |
| **Domain Event** | An in-process event published via Spring's `ApplicationEventPublisher` when a significant business action occurs (e.g., `LoanDisbursedEvent`, `RepaymentReceivedEvent`). Consumed by the notification module. | Not a message-broker event. In-process only for this scope. |

---

**Feeds into next document:** The glossary, assumptions A1–A20, and the bounded-context
hints here (Product Catalog, Customer, Origination, Servicing, Ledger, Notifications)
form the foundation for the domain model in [02-domain-model.md](02-domain-model.md).
