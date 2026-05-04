# 04 — Project Plan

> Repository structure, vertical-slice task breakdown, definition of done, and
> development workflow conventions.

---

## 1. Repository Structure

```
pa-lending-platform/
├── docs/                                  # This documentation package
│   ├── 01-assumptions-and-scope.md
│   ├── 02-domain-model.md
│   ├── 03-api-and-schema.md
│   ├── 04-project-plan.md
│   ├── 05-testing-strategy.md
│   ├── 06-trade-offs.md
│   ├── openapi.yaml
│   └── adr/
│       ├── 0001-hexagonal-architecture.md
│       ├── ...
│       └── 0010-testing-strategy.md
│
├── docker-compose.yml                     # PostgreSQL for local dev
├── build.gradle                           # Gradle build (Kotlin DSL)
├── settings.gradle.kts
├── gradle/
│   └── wrapper/
├── gradlew / gradlew.bat
│
├── src/
│   ├── main/
│   │   ├── java/com/lending/
│   │   │   ├── LendingApplication.java             # @SpringBootApplication
│   │   │   │
│   │   │   ├── shared/                              # Cross-cutting concerns
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Money.java                   # Value object
│   │   │   │   │   └── DomainEvent.java             # Marker interface
│   │   │   │   ├── adapter/
│   │   │   │   │   ├── in/web/
│   │   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   │   └── ErrorResponse.java
│   │   │   │   │   └── out/persistence/
│   │   │   │   │       ├── MoneyConverter.java       # JPA AttributeConverter
│   │   │   │   │       └── AuditEntityListener.java
│   │   │   │   ├── infrastructure/
│   │   │   │   │   ├── IdempotencyFilter.java
│   │   │   │   │   └── IdempotencyStore.java
│   │   │   │   └── config/
│   │   │   │       ├── JacksonConfig.java            # Money serialiser
│   │   │   │       └── SchedulingConfig.java
│   │   │   │
│   │   │   ├── product/                             # Product Catalog context
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── LoanProduct.java
│   │   │   │   │   │   ├── FeeDefinition.java
│   │   │   │   │   │   ├── FeeType.java
│   │   │   │   │   │   ├── FeeCalculationMethod.java
│   │   │   │   │   │   ├── RepaymentFrequency.java
│   │   │   │   │   │   ├── InterestAccrualMethod.java
│   │   │   │   │   │   ├── OriginationFeeChargeMethod.java
│   │   │   │   │   │   └── OverpaymentPolicy.java
│   │   │   │   │   └── event/
│   │   │   │   │       └── LoanProductCreatedEvent.java
│   │   │   │   ├── port/
│   │   │   │   │   ├── in/
│   │   │   │   │   │   ├── CreateProductUseCase.java
│   │   │   │   │   │   ├── UpdateProductUseCase.java
│   │   │   │   │   │   └── GetProductUseCase.java
│   │   │   │   │   └── out/
│   │   │   │   │       └── LoanProductRepository.java
│   │   │   │   └── adapter/
│   │   │   │       ├── in/web/
│   │   │   │       │   ├── ProductController.java
│   │   │   │       │   ├── CreateProductRequest.java
│   │   │   │       │   └── ProductResponse.java
│   │   │   │       └── out/persistence/
│   │   │   │           ├── LoanProductJpaEntity.java
│   │   │   │           ├── LoanProductJpaRepository.java
│   │   │   │           └── LoanProductMapper.java
│   │   │   │
│   │   │   ├── customer/                            # Customer context
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── Customer.java
│   │   │   │   │   └── event/
│   │   │   │   │       └── CustomerCreatedEvent.java
│   │   │   │   ├── port/
│   │   │   │   │   ├── in/ ...
│   │   │   │   │   └── out/ ...
│   │   │   │   └── adapter/
│   │   │   │       ├── in/web/ ...
│   │   │   │       └── out/persistence/ ...
│   │   │   │
│   │   │   ├── origination/                         # Loan Origination context
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── LoanApplication.java
│   │   │   │   │   │   └── ApplicationStatus.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── LoanApplicationService.java
│   │   │   │   │   │   └── EligibilityValidator.java
│   │   │   │   │   └── event/
│   │   │   │   │       ├── LoanApplicationApprovedEvent.java
│   │   │   │   │       └── LoanApplicationRejectedEvent.java
│   │   │   │   ├── port/ ...
│   │   │   │   └── adapter/ ...
│   │   │   │
│   │   │   ├── servicing/                           # Loan Servicing context
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── LoanAccount.java
│   │   │   │   │   │   ├── LoanState.java           # Enum state machine
│   │   │   │   │   │   ├── Installment.java
│   │   │   │   │   │   ├── InstallmentStatus.java
│   │   │   │   │   │   └── Repayment.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── LoanServicingService.java
│   │   │   │   │   │   ├── RepaymentAllocationService.java
│   │   │   │   │   │   ├── AmortisationCalculator.java
│   │   │   │   │   │   └── OverdueDetectionService.java
│   │   │   │   │   └── event/
│   │   │   │   │       ├── LoanDisbursedEvent.java
│   │   │   │   │       ├── RepaymentReceivedEvent.java
│   │   │   │   │       ├── LoanOverdueEvent.java
│   │   │   │   │       └── LoanClosedEvent.java
│   │   │   │   ├── port/ ...
│   │   │   │   └── adapter/ ...
│   │   │   │
│   │   │   ├── ledger/                              # Ledger context
│   │   │   │   ├── domain/
│   │   │   │   │   └── model/
│   │   │   │   │       ├── LedgerEntry.java
│   │   │   │   │       ├── LedgerEntryType.java
│   │   │   │   │       └── Direction.java
│   │   │   │   ├── port/ ...
│   │   │   │   └── adapter/
│   │   │   │       ├── in/
│   │   │   │       │   └── LedgerEventListener.java  # Listens to servicing events
│   │   │   │       └── out/persistence/ ...
│   │   │   │
│   │   │   └── notification/                        # Notifications context
│   │   │       ├── domain/
│   │   │       │   ├── model/
│   │   │       │   │   ├── NotificationRecord.java
│   │   │       │   │   ├── NotificationChannel.java  # Interface
│   │   │       │   │   └── NotificationStatus.java
│   │   │       │   └── service/
│   │   │       │       └── NotificationDispatcher.java
│   │   │       ├── port/ ...
│   │   │       └── adapter/
│   │   │           ├── in/
│   │   │           │   └── NotificationEventListener.java
│   │   │           └── out/
│   │   │               └── LoggingNotificationChannel.java  # Stub
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-test.yml
│   │       └── db/migration/
│   │           ├── V001__create_loan_products.sql
│   │           ├── V002__create_customers.sql
│   │           ├── V003__create_loan_applications.sql
│   │           ├── V004__create_loan_accounts.sql
│   │           ├── V005__create_installments.sql
│   │           ├── V006__create_repayments.sql
│   │           ├── V007__create_ledger_entries.sql
│   │           ├── V008__create_notification_records.sql
│   │           ├── V009__create_audit_log.sql
│   │           ├── V010__create_idempotency_store.sql
│   │           ├── V900__seed_loan_products.sql
│   │           ├── V901__seed_customers.sql
│   │           └── V902__seed_demo_loans.sql
│   │
│   └── test/
│       └── java/com/lending/
│           ├── shared/domain/MoneyTest.java
│           ├── servicing/domain/
│           │   ├── LoanStateTest.java
│           │   ├── RepaymentAllocationServiceTest.java
│           │   └── AmortisationCalculatorTest.java
│           ├── origination/domain/
│           │   └── EligibilityValidatorTest.java
│           ├── product/adapter/
│           │   └── ProductControllerIT.java
│           ├── servicing/adapter/
│           │   ├── RepaymentControllerIT.java
│           │   └── OverdueDetectionJobIT.java
│           └── e2e/
│               ├── LoanLifecycleE2ETest.java
│               └── OverdueFlowE2ETest.java
│
└── README.md
```

---

## 2. Vertical-Slice Task Breakdown

Each slice produces a demoable increment. Slices are ordered by dependency — each
builds on the previous. Time estimates assume a single developer.

### Slice 0: Project Skeleton (foundation)

| Task | Details |
|------|---------|
| Init Spring Boot project (Gradle, Java 21) | spring-boot-starter-web, spring-boot-starter-data-jpa, flyway-core, postgresql, springdoc-openapi, spring-boot-starter-test, testcontainers |
| `docker-compose.yml` with PostgreSQL 16 | Port 5432, default DB `lending`, user `lending`, password `lending` |
| `application.yml` configuration | Datasource, Flyway, Jackson, Actuator |
| `Money` value object + Jackson ser/de + JPA converter | Unit tests for arithmetic, equality, serialisation |
| `GlobalExceptionHandler` + `ErrorResponse` DTO | Consistent error envelope |
| `IdempotencyFilter` + `idempotency_store` migration | Integration test proving replay returns stored response |
| `AuditEntityListener` + `audit_log` migration | |

**Demo:** `./gradlew bootRun` starts, connects to PostgreSQL, Flyway runs, `/actuator/health` returns UP. Money and idempotency have passing tests.

### Slice 1: Product Catalog

| Task | Details |
|------|---------|
| `LoanProduct` domain model + `FeeDefinition` VO | |
| `LoanProductRepository` port + JPA adapter | |
| `ProductController` — full CRUD | POST, GET, GET/{id}, PUT, DELETE |
| Flyway migration `V001` | |
| Seed data `V900` (2 products: short-term consumer, long-term installment) | |
| Unit tests: product validation invariants | |
| Integration test: CRUD + idempotency | |

**Demo:** Create, list, update, soft-delete products via curl/Swagger. Idempotent POST replay returns same product.

### Slice 2: Customer Management

| Task | Details |
|------|---------|
| `Customer` domain model | |
| Repository port + JPA adapter | |
| `CustomerController` — CRUD + list customer loans | |
| Flyway migration `V002` | |
| Seed data `V901` (3 customers with varying credit limits) | |
| Unit tests: credit limit invariants | |
| Integration test: CRUD + duplicate email/phone rejection | |

**Demo:** Create customers, query by ID, see empty loan list.

### Slice 3: Loan Origination

| Task | Details |
|------|---------|
| `LoanApplication` domain model + `ApplicationStatus` enum | |
| `EligibilityValidator` domain service | |
| `LoanApplicationService` — submit, approve, reject | |
| `ApplicationController` | |
| Flyway migration `V003` | |
| Domain events: `LoanApplicationApprovedEvent`, `LoanApplicationRejectedEvent` | |
| Unit tests: eligibility rules (credit limit, max loans, product bounds) | |
| Integration test: submit → approve → verify LoanAccount created | |

**Demo:** Submit application, approve it, see it transition to APPROVED. Reject an ineligible application with a descriptive reason.

### Slice 4: Loan Servicing — Disbursement & Schedule

| Task | Details |
|------|---------|
| `LoanAccount` domain model + `LoanState` enum state machine | |
| `AmortisationCalculator` — EMI formula | |
| `Installment` entity | |
| `LoanServicingService.disburseLoan()` | |
| `LoanController` + `ScheduleController` | |
| Flyway migrations `V004`, `V005` | |
| Seed data `V902` (1 active loan with full schedule) | |
| Unit tests: state machine transitions, EMI calculation (including edge cases) | |
| Integration test: disburse → verify schedule generated | |

**Demo:** Approve + disburse a loan. View the generated amortisation schedule. Attempt to disburse again → 409.

### Slice 5: Repayments & Ledger

| Task | Details |
|------|---------|
| `Repayment` entity | |
| `RepaymentAllocationService` — FIFO allocation, interest before principal | |
| `RepaymentController` | |
| `LedgerEntry` model + `LedgerEventListener` | |
| Flyway migrations `V006`, `V007` | |
| Unit tests: allocation (full, partial, overpayment rejection, multi-installment) | |
| Integration test: record repayment → verify installment update + ledger entries | |
| E2E test: full lifecycle (apply → approve → disburse → repay all → CLOSED) | |

**Demo:** Record repayments against an active loan. View updated schedule, ledger entries. Final repayment closes the loan.

### Slice 6: Overdue Detection

| Task | Details |
|------|---------|
| `OverdueDetectionService` — scheduled job | |
| Late fee application logic | |
| Integration test: advance clock, run job, verify OVERDUE state + fee + ledger | |
| E2E test: loan goes overdue, repayment clears it, loan returns to ACTIVE | |

**Demo:** Seed a loan with a past-due installment. Trigger overdue detection. Loan transitions to OVERDUE. Late fee appears in ledger.

### Slice 7: Notifications (stub)

| Task | Details |
|------|---------|
| `NotificationEventListener` — listens to all lifecycle events | |
| `NotificationRecord` entity + repository | |
| `LoggingNotificationChannel` — logs to console | |
| `NotificationChannel` interface (extensibility point) | |
| Flyway migration `V008` | |
| Integration test: disburse loan → verify NotificationRecord persisted | |

**Demo:** Walk through the full lifecycle; notification records are created for every event. Logs show "NOTIFICATION [EMAIL]: Your loan has been disbursed..."

### Slice 8: Polish & Documentation

| Task | Details |
|------|---------|
| README — fill in quickstart, architecture diagram, setup instructions | |
| Verify all seed data tells a coherent story | |
| Run full test suite, fix flaky tests | |
| Review OpenAPI spec matches implementation | |
| Final code review pass — remove TODOs, dead code | |

---

## 3. Definition of Done (for the case study deliverable)

A slice is "done" when all of the following are true:

- [ ] All endpoints for the slice are implemented and return correct responses.
- [ ] Flyway migration creates the required tables and constraints.
- [ ] Unit tests pass for all domain logic.
- [ ] Integration tests pass with Testcontainers PostgreSQL.
- [ ] Idempotency works on all mutating endpoints (tested).
- [ ] Audit log entries are created for state changes.
- [ ] Domain events are published and consumed by downstream listeners.
- [ ] Code compiles with zero warnings (excluding deprecation warnings from dependencies).
- [ ] `./gradlew test` passes with zero failures.
- [ ] `./gradlew bootRun` starts successfully against `docker-compose up`.

**For the final deliverable:**

- [ ] `docker-compose up && ./gradlew bootRun` works from a clean checkout.
- [ ] Seed data demonstrates the three key flows (see sequence diagrams in [03-api-and-schema.md](03-api-and-schema.md)).
- [ ] Swagger UI at `/swagger-ui.html` shows all endpoints.
- [ ] README contains setup instructions, architecture overview, and links to docs.
- [ ] All ADRs are committed to `docs/adr/`.

---

## 4. Commit Hygiene

### Conventional Commits

All commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(scope): <short description>

[optional body]
[optional footer]
```

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`  
**Scopes:** `product`, `customer`, `origination`, `servicing`, `ledger`, `notification`, `shared`, `infra`

**Examples:**
```
feat(product): add CRUD endpoints for loan products
test(servicing): add unit tests for EMI amortisation calculator
docs(adr): add ADR-0004 state machine for loan lifecycle
fix(servicing): handle rounding remainder in last installment
refactor(shared): extract Money JPA converter to shared module
chore(infra): add docker-compose.yml for PostgreSQL
```

### Atomic Commits

Each commit should:
- Contain exactly one logical change.
- Leave the codebase in a compilable, test-passing state.
- Not mix refactoring with feature work.

---

## 5. Branching / PR Strategy

Even as a solo repository, branches and PRs provide structure and signal professionalism.

```
main                    ← always deployable, protected
  ├── feat/slice-0-skeleton
  ├── feat/slice-1-products
  ├── feat/slice-2-customers
  ├── feat/slice-3-origination
  ├── feat/slice-4-servicing
  ├── feat/slice-5-repayments
  ├── feat/slice-6-overdue
  ├── feat/slice-7-notifications
  └── feat/slice-8-polish
```

- One branch per slice. Merged to `main` via squash-merge PR.
- PR description references the slice from this plan.
- All tests must pass before merge (enforced by running `./gradlew test` locally;
  CI pipeline is out of scope but the test commands are ready for one).

---

**Feeds into next document:** The vertical slices define what needs to be tested. The
testing strategy in [05-testing-strategy.md](05-testing-strategy.md) maps specific
painful test cases to these slices.
