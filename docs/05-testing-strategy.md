# 05 — Testing Strategy

> Detailed test plan covering the test pyramid, specific painful edge cases, and
> local execution instructions. For the architectural rationale behind this strategy,
> see [ADR-0010](adr/0010-testing-strategy.md).

---

## 1. Test Pyramid Overview

```
        ┌───────────────┐
        │   E2E Tests   │  ~10%  (2-3 tests, full HTTP, Testcontainers)
        │  (*E2E.java)  │
        ├───────────────┤
        │  Integration  │  ~30%  (adapters, controllers, Flyway, idempotency)
        │  (*IT.java)   │
        ├───────────────┤
        │  Unit Tests   │  ~60%  (domain logic, value objects, state machine)
        │ (*Test.java)  │
        └───────────────┘
```

### Layer Boundaries

| Layer | Spring Context? | Database? | Docker? | Speed |
|-------|:-:|:-:|:-:|-------|
| Unit | No | No | No | <1 s per class |
| Integration | Yes (`@SpringBootTest` or `@DataJpaTest`) | Yes (Testcontainers PG) | Yes | 5–30 s per class |
| E2E | Yes (`RANDOM_PORT`) | Yes (Testcontainers PG) | Yes | 10–60 s per class |

---

## 2. Unit Tests — Domain Logic

### 2.1 `MoneyTest`

| Case | Input | Expected |
|------|-------|----------|
| Addition, same currency | `USD 100.0000 + USD 50.5000` | `USD 150.5000` |
| Addition, different currency | `USD 100 + EUR 50` | `CurrencyMismatchException` |
| Subtraction yielding zero | `USD 100 - USD 100` | `USD 0.0000` |
| Subtraction yielding negative | `USD 50 - USD 100` | `NegativeMoneyException` (in balance context) |
| Scale normalisation | `new Money("10", USD)` | Internal amount is `10.0000` |
| Rounding (HALF_EVEN) | `USD 10.0000 / 3` | `USD 3.3333` (banker's rounding) |
| Equality: same value different scale | `Money("10.00") vs Money("10.0000")` | Equal (compareTo, not equals) |
| JSON round-trip | Serialise → deserialise | `amount` is string `"10.0000"`, `currency` is `"USD"` |
| Zero check | `Money.ZERO` | `isZero()` returns true |
| Multiply by percentage | `USD 10000.0000 * 0.025` | `USD 250.0000` |

### 2.2 `LoanStateTest`

| Case | From | To | Expected |
|------|------|----|----------|
| Valid: disburse | `PENDING_DISBURSEMENT` | `ACTIVE` | Success |
| Valid: overdue | `ACTIVE` | `OVERDUE` | Success |
| Valid: close from active | `ACTIVE` | `CLOSED` | Success |
| Valid: cure overdue | `OVERDUE` | `ACTIVE` | Success |
| Valid: close from overdue | `OVERDUE` | `CLOSED` | Success |
| Valid: default | `OVERDUE` | `DEFAULTED` | Success |
| Valid: write off | `DEFAULTED` | `WRITTEN_OFF` | Success |
| Invalid: skip disbursement | `PENDING_DISBURSEMENT` | `OVERDUE` | `IllegalLoanStateTransitionException` |
| Invalid: reopen closed | `CLOSED` | `ACTIVE` | `IllegalLoanStateTransitionException` |
| Invalid: close written off | `WRITTEN_OFF` | `CLOSED` | `IllegalLoanStateTransitionException` |
| Exhaustive: every invalid pair | All non-valid pairs | `IllegalLoanStateTransitionException` |

### 2.3 `AmortisationCalculatorTest`

| Case | Principal | Rate | Tenure | Expected |
|------|-----------|------|--------|----------|
| Standard EMI (12 months) | `USD 12000` | 12% APR | 12 mo | 12 installments, EMI ≈ `USD 1066.19`, total interest ≈ `USD 794.25` |
| Short loan (1 month) | `USD 1000` | 12% APR | 1 mo | 1 installment, interest = `USD 10.00` |
| Zero interest | `USD 6000` | 0% | 6 mo | Equal principal splits: `USD 1000.00` each, zero interest |
| Rounding remainder | `USD 10000` | 10% APR | 3 mo | Last installment absorbs rounding remainder so `sum(installments) == principal + total_interest` exactly |
| **Leap year** | `USD 12000` | 12% APR | 12 mo, starting 2028-01-15 | Feb installment due 2028-02-15 (leap year); schedule handles 29-day month |
| **Month-end rollover** | Start 2026-01-31 | any | 6 mo | Due dates: Jan 31, Feb 28, Mar 31, Apr 30, May 31, Jun 30 |
| Weekly frequency | `USD 5200` | 10% APR | 52 wk | 52 installments, weekly EMI |
| Bi-weekly frequency | `USD 5200` | 10% APR | 26 bi-wk | 26 installments |

### 2.4 `RepaymentAllocationServiceTest`

| Case | Amount | Schedule State | Expected |
|------|--------|---------------|----------|
| **Full single installment** | `USD 1066.19` | Installment 1 pending | Installment 1 → `PAID`, interest allocated first |
| **Partial payment** | `USD 500.00` | Installment 1 pending (total due 1066.19) | Interest portion fully paid (`104.17`), remainder (`395.83`) to principal. Installment → `PARTIALLY_PAID` |
| **Overpayment spanning 2 installments** | `USD 2000.00` | Installments 1,2 pending | Installment 1 fully paid, remainder allocated to installment 2 |
| **Exact payoff** | Remaining balance | 3 installments pending | All installments → `PAID`, loan → `CLOSED` |
| **Overpayment beyond balance (REJECT policy)** | Balance + `USD 1.00` | Any, overpaymentPolicy=REJECT | `ExcessRepaymentException` |
| **Overpayment beyond balance (APPLY_TO_NEXT)** | Balance + `USD 1.00` | Any, overpaymentPolicy=APPLY_TO_NEXT_INSTALLMENT | All installments paid, surplus allocated to next; if no next, loan → `CLOSED` and surplus rejected |
| **Overpayment beyond balance (HOLD_AS_CREDIT)** | Balance + `USD 50.00` | Any, overpaymentPolicy=HOLD_AS_CREDIT | All installments paid, `creditBalance` = `USD 50.00`, loan → `CLOSED` |
| **Zero amount** | `USD 0` | Any | Validation error (amount must be > 0) |
| **Payment to partially paid installment** | `USD 700.00` | Installment 1 partially paid (already paid `500.00` of `1066.19`) | Remaining `566.19` allocated to installment 1 (→ `PAID`), surplus `133.81` to installment 2 |
| **Early payoff with prepayment penalty** | Full remaining balance | Product has `PREPAYMENT_PENALTY` at 2% of outstanding | Penalty computed, added to payoff amount, penalty ledger entry recorded |
| **Early payoff when `allowEarlyRepayment` = false** | Full remaining balance | Before last installment due date | `EarlyRepaymentNotAllowedException` |

### 2.5 `EligibilityValidatorTest`

| Case | Expected |
|------|----------|
| All conditions met | Eligible |
| Requested principal below product minimum | Rejected: "below minimum principal" |
| Requested principal above product maximum | Rejected: "exceeds maximum principal" |
| Tenure below product minimum | Rejected: "below minimum tenure" |
| Tenure above product maximum | Rejected: "exceeds maximum tenure" |
| Product inactive | Rejected: "product is not active" |
| Customer deleted | Rejected: "customer not found" |
| Outstanding + requested > credit limit | Rejected: "exceeds credit limit" |
| Active loan count = max_active_loans | Rejected: "maximum active loans reached" |
| Credit limit exactly met (boundary) | Eligible (≤, not <) |

---

## 3. Integration Tests — Adapters

### 3.1 Repository Tests

| Test | Verifies |
|------|----------|
| `LoanProductJpaRepositoryIT.save_and_findById` | Round-trip persistence, Money columns stored as NUMERIC(19,4) |
| `LoanProductJpaRepositoryIT.findByActiveTrue_excludesDeleted` | Soft-delete filtering works |
| `LoanProductJpaRepositoryIT.uniqueNameConstraint` | Duplicate name → `DataIntegrityViolationException` |
| `CustomerJpaRepositoryIT.uniqueEmailConstraint` | Duplicate email → exception |
| `InstallmentJpaRepositoryIT.findOverdueByLoanAndDate` | Query returns installments where `due_date < :today AND status != PAID` |

### 3.2 Controller Tests (MockMvc)

| Test | Verifies |
|------|----------|
| `ProductControllerIT.createProduct_returns201` | Happy path, response shape matches OpenAPI spec |
| `ProductControllerIT.createProduct_missingName_returns400` | Validation error with field-level detail |
| `ProductControllerIT.createProduct_idempotencyReplay` | Same `Idempotency-Key` → same response, `X-Idempotent-Replayed: true` header |
| `RepaymentControllerIT.recordRepayment_toLoanNotActive_returns409` | State guard works at HTTP level |
| `RepaymentControllerIT.recordRepayment_exceedsBalance_returns422` | Business rule violation |
| `ApplicationControllerIT.approve_ineligible_returns422` | Eligibility failure with descriptive message |

### 3.3 Idempotency Filter Test

| Test | Verifies |
|------|----------|
| `IdempotencyFilterIT.missingHeader_returns400` | Mutating request without Idempotency-Key is rejected |
| `IdempotencyFilterIT.getRequest_noHeaderRequired` | GET requests pass through without the header |
| `IdempotencyFilterIT.replay_returnsStoredResponse` | Second POST with same key returns identical response |
| `IdempotencyFilterIT.expiredKey_returns409` | Key past TTL returns conflict |
| `IdempotencyFilterIT.concurrentRequests_onlyOneExecutes` | Two simultaneous POSTs with same key → one executes, one replays |

### 3.4 Flyway Migration Test

| Test | Verifies |
|------|----------|
| `FlywayMigrationIT.allMigrationsApplyCleanly` | Fresh database → all V* migrations run without error |
| `FlywayMigrationIT.seedDataLoads` | V900–V902 seed migrations produce expected row counts |

### 3.5 Overdue Detection Job Test

| Test | Verifies |
|------|----------|
| `OverdueDetectionJobIT.activeLoanWithPastDueInstallment_becomesOverdue` | State transitions to OVERDUE, installment status updated |
| `OverdueDetectionJobIT.activeLoanWithNoOverdue_unchanged` | Loan remains ACTIVE |
| `OverdueDetectionJobIT.alreadyOverdueLoan_notDoubleProcessed` | Job is idempotent for already-overdue loans |
| `OverdueDetectionJobIT.lateFeeApplied` | Late fee added to outstanding balance, ledger entry created |

---

## 4. End-to-End Tests — Golden Path

### 4.1 `LoanLifecycleE2ETest`

Full lifecycle via HTTP:

1. `POST /products` — create a loan product
2. `POST /customers` — create a customer
3. `POST /loans/applications` — submit application
4. `POST /loans/applications/{id}/approve` — approve
5. `GET /loans/{id}` — verify `PENDING_DISBURSEMENT`
6. `POST /loans/{id}/disburse` — disburse
7. `GET /loans/{id}/schedule` — verify 6 installments
8. `GET /loans/{id}/ledger` — verify DISBURSEMENT entry
9. `POST /loans/{id}/repayments` × 6 — pay each installment
10. `GET /loans/{id}` — verify `CLOSED`, outstanding balance = 0
11. `POST /loans/{id}/repayments` — attempt payment on closed loan → 409
12. `GET /loans/{id}/ledger` — verify complete ledger (1 disbursement + 12 repayment entries)

### 4.2 `OverdueFlowE2ETest`

1. Create product with `LATE_PAYMENT_FEE`
2. Create customer + submit + approve + disburse loan
3. Advance system clock past first installment due date (use `Clock` bean injection)
4. Trigger overdue detection job manually
5. Verify loan state = `OVERDUE`
6. Verify late fee appears in ledger
7. Record repayment covering overdue installment + late fee
8. Verify loan state returns to `ACTIVE` (or `CLOSED` if fully paid)

---

## 5. Specific Painful Cases to Cover

These are the cases that distinguish a production-quality lending system from a toy:

### 5.1 Date Math

| Case | Why it's painful | How to test |
|------|-----------------|-------------|
| **Leap year (Feb 29)** | Loan disbursed Jan 31 → Feb installment due date? Feb 28 in non-leap, Feb 29 in leap year. | Use `Clock` fixed to 2028-01-31 (2028 is leap). Verify Feb due date is 2028-02-29. |
| **Month-end rollover** | Loan disbursed Jan 31 → next due dates must handle months with <31 days. | Disbursement on Jan 31: verify Feb 28/29, Mar 31, Apr 30 due dates. |
| **Repayment on exact due date** | Is the installment considered "on time" or "overdue"? | Pay on due date, then run overdue job. Installment should be `PAID`, not `OVERDUE`. |
| **Repayment one day late (zero grace period)** | Overdue detection must have run before the repayment. | Product with `gracePeriodDays=0`. Run overdue job (marks `OVERDUE`), then repay. Verify late fee was charged. |
| **Repayment within grace period** | With `gracePeriodDays=3`, payment 2 days after due date is still "on time". | Product with `gracePeriodDays=3`. Pay 2 days after due date. Run overdue job. Installment should remain `PENDING` or `PAID`, NOT `OVERDUE`. |
| **Repayment after grace period expires** | With `gracePeriodDays=3`, payment 4 days after due date is overdue. | Product with `gracePeriodDays=3`. Run overdue job on due date + 4. Loan transitions to `OVERDUE`. Late fee charged. |

### 5.2 Money Edge Cases

| Case | Why it's painful |
|------|-----------------|
| **Rounding remainder in last installment** | EMI × N ≠ total due exactly. Last installment must absorb the difference. |
| **Sub-cent allocation** | When allocating a small payment across interest/principal, intermediate values may have >4 decimals. Must round correctly. |
| **Zero-interest product** | Division by zero in interest calculation if not handled. |

### 5.3 Concurrency

| Case | Why it's painful | How to test |
|------|-----------------|-------------|
| **Concurrent repayments to the same loan** | Two threads submit repayments simultaneously. Without locking, both could read the same outstanding balance and both succeed, double-crediting the loan. | Integration test: use `ExecutorService` to submit 2 repayments simultaneously. One succeeds, one fails with optimistic lock exception (or the second acquires the pessimistic lock and sees the updated balance). |
| **Concurrent approve on same application** | Two threads approve the same application. Without idempotency or locking, two `LoanAccount`s could be created. | Integration test: same `Idempotency-Key` → one executes, one replays. Different keys → one succeeds, one gets `409 Conflict` (application already approved). |

### 5.4 Idempotency Replay

| Case | Expected |
|------|----------|
| Replay same repayment (same idempotency key) | Returns original `201` response, no additional ledger entries, balance unchanged |
| Different amount with same key | Returns original response (key is bound to first request) |
| Same amount with different key | Creates a new repayment (keys are distinct) |
| Replay after loan state change (e.g., loan now CLOSED) | Returns original `201` response (idempotency store is authoritative, not current state) |

### 5.5 Business Rule Boundaries

| Case | Expected |
|------|----------|
| Request principal = product `maxPrincipal` exactly | Accepted (boundary inclusive) |
| Request principal = product `maxPrincipal` + `0.0001` | Rejected |
| Outstanding + requested = `creditLimit` exactly | Accepted |
| Customer with `maxActiveLoans` = 1 and 1 active loan | Rejected |
| Customer with `maxActiveLoans` = 1 and 1 CLOSED loan | Accepted (CLOSED is not active) |
| Disburse loan for `DAILY_ACCRUAL` product | `501 Not Implemented` (v1 limitation) |
| Product with `allowEarlyRepayment=false` and `PREPAYMENT_PENALTY` in feeSchedule | Validation error at product creation (contradiction) |
| Product with `PREPAYMENT_PENALTY` using `PERCENTAGE_OF_PRINCIPAL` method | Validation error (only `FLAT` or `PERCENTAGE_OF_OUTSTANDING` allowed) |
| Origination fee `DEDUCTED_FROM_DISBURSEMENT`: verify `totalDisbursed = principal - fee` | Correct disbursement amount |
| Origination fee `ADDED_TO_BALANCE`: verify `totalDisbursed = principal`, `outstandingBalance` includes fee | Correct balance calculation |

---

## 6. Running Tests Locally

### Prerequisites

- Java 21 (JDK)
- Docker (for Testcontainers — required for integration and E2E tests)
- No running PostgreSQL needed (Testcontainers manages its own container)

### Commands

```bash
# Run all tests
./gradlew test

# Run only unit tests (no Docker needed)
./gradlew test --tests 'com.lending.**.*Test'

# Run only integration tests (Docker required)
./gradlew test --tests 'com.lending.**.*IT'

# Run only E2E tests (Docker required)
./gradlew test --tests 'com.lending.**.*E2E'

# Run a specific test class
./gradlew test --tests 'com.lending.servicing.domain.RepaymentAllocationServiceTest'

# Run with verbose output
./gradlew test --info
```

### Seed Data and Tests

Seed data (`V900`–`V902`) is **NOT** used by tests. Tests are fully self-contained:

- **Unit tests** create domain objects directly via constructors.
- **Integration tests** persist test data via repositories at the start of each test.
- **E2E tests** create all data via HTTP API calls.

This ensures:
1. Tests are independent of seed data changes.
2. Tests can run in any order.
3. Seed data changes don't break the test suite.

Seed data exists solely for:
- Manual exploration via curl / Swagger UI.
- Demonstrating the system's key flows to reviewers.

---

**Feeds into next document:** Testing reveals what's well-covered and what's deliberately
skipped. The trade-offs in [06-trade-offs.md](06-trade-offs.md) document what's stubbed,
what would be built next, and what the known limitations are.
