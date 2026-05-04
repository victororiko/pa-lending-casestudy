# ADR-0010: Testing Strategy

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

The case study brief says: "Treat this as you would treat a production codebase that
other engineers will read and extend." A production codebase has tests. The test strategy
must:

1. Cover the domain logic (state machine, repayment allocation, eligibility rules,
   amortisation calculation) thoroughly.
2. Verify that adapters (JPA repositories, REST controllers) work correctly with real
   infrastructure.
3. Provide a golden-path end-to-end test that exercises the full loan lifecycle.
4. Run quickly enough that developers execute them on every commit.

## Decision

Adopt a **test pyramid** strategy with three layers:

### Layer 1: Unit Tests (domain logic — fast, no Spring, no database)

- **What:** Pure Java/JUnit 5 tests for domain entities, value objects, domain services,
  and the state machine.
- **Dependencies:** JUnit 5, AssertJ, Mockito for port interfaces.
- **Examples:**
  - `MoneyTest` — arithmetic, currency mismatch, rounding, scale.
  - `LoanStateTest` — all valid transitions succeed, all invalid transitions throw.
  - `RepaymentAllocationServiceTest` — FIFO allocation, partial payments, overpayment rejection.
  - `AmortisationCalculatorTest` — EMI formula, edge cases (1-month loan, 360-month loan, zero interest).
  - `EligibilityValidatorTest` — credit limit enforcement, max active loans.
- **Convention:** Domain tests never import anything from `org.springframework`. If they
  do, the hexagonal boundary has been violated.
- **Target:** ~60% of all tests are unit tests.

### Layer 2: Integration Tests (adapters — Spring context + Testcontainers PostgreSQL)

- **What:** Tests that verify JPA repositories, REST controllers, Flyway migrations,
  and the idempotency filter work correctly with real PostgreSQL.
- **Dependencies:** Spring Boot Test, Testcontainers (PostgreSQL), MockMvc.
- **Examples:**
  - `LoanProductRepositoryIT` — CRUD, unique constraint on name, soft delete filtering.
  - `RepaymentControllerIT` — HTTP request → response, validation errors, idempotency replay.
  - `FlywayMigrationIT` — all migrations apply cleanly to a fresh database.
  - `OverdueDetectionJobIT` — verify the scheduled job transitions loans correctly.
- **Convention:** Integration tests use `@SpringBootTest` with `@Testcontainers`.
  Test class names end in `IT` (e.g., `LoanAccountRepositoryIT`).
- **Target:** ~30% of all tests are integration tests.

### Layer 3: End-to-End Tests (golden path — full application)

- **What:** A small number of tests that exercise the complete loan lifecycle via HTTP:
  create product → create customer → submit application → approve → disburse →
  make repayments → loan closes.
- **Dependencies:** Spring Boot Test, Testcontainers, `TestRestTemplate` or `WebTestClient`.
- **Examples:**
  - `LoanLifecycleE2ETest` — golden path from application to closure.
  - `OverdueFlowE2ETest` — loan goes overdue, late fee applied, then repaid.
- **Convention:** E2E tests use `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
  Test class names end in `E2E`.
- **Target:** ~10% of all tests are E2E tests.

### Test Data Strategy

- **Seed data** (`V900__seed_*.sql`) is for manual exploration and demo purposes only.
  Tests do NOT depend on seed data.
- Each test creates its own data via the API or repository, ensuring test isolation.
- Integration and E2E tests use `@Transactional` rollback OR fresh database state via
  Testcontainers (each test class gets a clean container).

### Running Tests

```bash
# All tests
./gradlew test

# Unit tests only (fast — no Spring, no Docker)
./gradlew test --tests '*Test'

# Integration tests only (requires Docker for Testcontainers)
./gradlew test --tests '*IT'

# E2E tests only
./gradlew test --tests '*E2E'
```

## Consequences

### Positive

- Domain logic is tested in milliseconds (no Spring boot, no database).
- Integration tests use real PostgreSQL via Testcontainers — no H2 dialect surprises.
- Test isolation: no shared mutable state between tests.
- Clear naming convention (`Test`, `IT`, `E2E`) makes it obvious what each test covers.

### Negative

- Testcontainers requires Docker on the developer's machine. Documented in README.
- Integration tests are slower (~10–30s per test class for container startup). Mitigated
  by Testcontainers' container reuse feature.
- Maintaining three test layers requires discipline. The pyramid can invert if
  developers default to E2E tests for everything.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **H2 for integration tests** | Fast but hides PostgreSQL-specific bugs (JSON columns, `NUMERIC` precision, locking behaviour). The whole point of integration tests is to verify real infrastructure behaviour. |
| **No E2E tests — rely on Postman collection** | Postman tests are not version-controlled (without Newman), not part of CI, and require manual setup. Automated E2E tests are reproducible. |
| **Full E2E only (no unit/integration split)** | Slow, brittle, hard to debug. A failing E2E test gives poor localisation — is it the controller, the service, the repository, or the database? The pyramid provides graduated feedback. |
| **Contract tests (Pact)** | Useful for microservice boundaries. Overkill for a monolith with no external consumers. |

---

For detailed test cases and painful edge cases, see [05-testing-strategy.md](../05-testing-strategy.md).
