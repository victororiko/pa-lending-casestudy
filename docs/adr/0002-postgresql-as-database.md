# ADR-0002: PostgreSQL as Database

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

The lending platform requires a database that:

1. Provides strong transactional guarantees — financial data cannot tolerate lost writes
   or phantom reads.
2. Supports complex relational queries — loans reference customers, products, schedules,
   and ledger entries.
3. Enforces data integrity at the schema level — `NOT NULL`, `UNIQUE`, `CHECK`
   constraints, foreign keys.
4. Handles `NUMERIC(19,4)` precision for monetary values without floating-point drift.
5. Is well-supported in the Spring Boot / JPA ecosystem.
6. Is easy to run locally for the "run from scratch on any machine" deliverable.

## Decision

Use **PostgreSQL** (latest stable, currently 16.x) as the sole database.

- Run locally via Docker Compose (`docker-compose.yml` provides a pre-configured
  PostgreSQL container).
- Schema managed via Flyway (see [ADR-0003](0003-flyway-for-migrations.md)).
- All monetary columns use `NUMERIC(19,4)`.
- All primary keys are `UUID` (generated in application code, not auto-increment).
- All tables include `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`, and
  `version BIGINT` columns.

## Consequences

### Positive

- ACID transactions with `SERIALIZABLE` or `READ COMMITTED` isolation as needed.
- Native `NUMERIC` type avoids floating-point issues for money.
- `JSONB` available for semi-structured data (audit log snapshots, notification payloads).
- Massive ecosystem: `pgAdmin`, `psql`, excellent Spring Data JPA support.
- Docker image is ~400 MB and starts in seconds.

### Negative

- Requires Docker or a local PostgreSQL installation. Mitigated by providing
  `docker-compose.yml`.
- Operational overhead compared to H2 for testing. Mitigated by using Testcontainers
  for integration tests (see [ADR-0010](0010-testing-strategy.md)).
- Single point of failure in this architecture (no read replicas, no sharding). Acceptable
  for case study scope.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **H2 (embedded)** | Zero-ops for development, but dialect differences cause subtle bugs in production. `NUMERIC` handling, `JSONB`, and locking behaviour differ. Using the same database in dev and prod eliminates an entire class of bugs. |
| **MySQL / MariaDB** | Viable, but PostgreSQL's stricter type system, better `NUMERIC` handling, and superior `JSONB` support make it a better fit for financial data. |
| **MongoDB** | Document store is a poor fit for relational lending data. Loans, schedules, and ledger entries have strong relationships. Transactions across collections are possible but awkward. |
| **SQLite** | No concurrent write support. Not suitable even for a case study that demonstrates repayment concurrency. |
