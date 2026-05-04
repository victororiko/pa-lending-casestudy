# ADR-0009: Audit Logging Strategy

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

Lending systems are subject to regulatory audit requirements. Even in a case-study
context, demonstrating auditability signals production-readiness. The system must answer:

- Who changed this customer's credit limit, and when?
- What was the loan's state before this transition?
- Which repayments were applied to which installments?

Three levels of auditability are relevant:

1. **Entity-level audit** — who created/updated each entity, when.
2. **Change-level audit** — what changed (before/after snapshots).
3. **Financial-level audit** — the ledger (append-only record of all monetary movements).

## Decision

Implement a **three-tier audit strategy**:

### Tier 1: Audit Columns (on every entity)

Every table includes:

```sql
created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
created_by   VARCHAR(100) NOT NULL DEFAULT 'system',
version      BIGINT NOT NULL DEFAULT 0
```

- `created_by` is populated from the `X-User-Id` header (see [ADR-0008](0008-authentication-scope.md)).
- `version` is used for optimistic locking (`@Version` in JPA).
- `updated_at` is auto-maintained by a JPA `@PreUpdate` listener.

### Tier 2: Audit Log Table (change tracking)

An `audit_log` table captures before/after snapshots for significant entity changes:

```sql
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(20) NOT NULL,  -- CREATE, UPDATE, STATE_CHANGE, DELETE
    before_snapshot JSONB,
    after_snapshot  JSONB NOT NULL,
    changed_fields  TEXT[],                -- list of field names that changed
    performed_by    VARCHAR(100) NOT NULL,
    performed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_performed_at ON audit_log (performed_at);
```

- Implemented via a **JPA `@EntityListener`** or a domain-level aspect that intercepts
  save operations.
- `before_snapshot` is null for `CREATE` actions.
- `changed_fields` provides a quick summary without diffing JSON.
- Snapshots are serialised via Jackson, excluding circular references.

### Tier 3: Append-Only Ledger (financial audit)

The `ledger_entries` table (see [02-domain-model.md](../02-domain-model.md)) serves as
the financial audit trail. Every monetary movement — disbursement, repayment allocation,
fee charge — is recorded as an immutable entry. The ledger is the authoritative record
of "what money moved, when, and why."

The ledger and the audit log serve different purposes:
- **Audit log** answers "who changed what entity state."
- **Ledger** answers "what money moved and why."

### What is NOT built:

- Audit log querying API (an admin UI concern).
- Tamper-proof audit log (would require hash chaining or an append-only database).
  See [06-trade-offs.md](../06-trade-offs.md).
- Compliance-specific audit reports (SOC 2, PCI). These are operational concerns.

## Consequences

### Positive

- Every entity change is traceable to a user and timestamp.
- State transitions are recorded with before/after snapshots.
- The ledger provides a complete financial history per loan.
- Audit data is queryable via standard SQL (no separate audit system to deploy).

### Negative

- Audit log table will grow unboundedly. For a case study, this is fine. In production,
  partition by `performed_at` or archive to cold storage.
- `JSONB` snapshots consume significant storage for large entities. Mitigated by only
  auditing significant entities (LoanAccount, Customer, LoanProduct), not all tables.
- JPA entity listener adds a small overhead to every save. Negligible for case-study
  throughput.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **Hibernate Envers** | Full audit history with minimal code. Rejected because: (a) it creates shadow tables (`_AUD`) for every audited entity, adding schema complexity; (b) it couples the audit strategy to Hibernate, violating the hexagonal architecture goal; (c) querying Envers audit data requires its proprietary API. A simple `audit_log` table with JSON snapshots is more portable and transparent. |
| **Event sourcing** | Every state change is an event; current state is a projection. Provides perfect auditability but adds enormous complexity: event store, projections, eventual consistency, schema evolution for events. The append-only ledger gives us financial-level event sourcing where it matters most, without the full overhead. |
| **CDC (Change Data Capture) via Debezium** | Captures database changes at the WAL level. Zero application code changes. But adds infrastructure (Kafka + Debezium connector) and provides raw row-level changes without business context. We want "loan state changed from ACTIVE to OVERDUE by overdue-detection-job," not "column `state` changed from `ACTIVE` to `OVERDUE`." |
| **No audit — rely on application logs** | Application logs are unstructured, unsearchable, and impermanent. Not acceptable for financial systems. |
