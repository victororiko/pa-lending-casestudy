# ADR-0003: Flyway for Database Migrations

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

The case study requires "database schema or migration scripts" as a deliverable. The
schema will evolve during development and must be reproducible on any machine. We need
a migration tool that:

1. Tracks which migrations have been applied (no double-apply risk).
2. Runs automatically on application startup (zero manual steps).
3. Supports plain SQL migrations (readable, reviewable, version-controllable).
4. Integrates with Spring Boot out of the box.

## Decision

Use **Flyway** (Community Edition) for database migrations.

- Migration files live in `src/main/resources/db/migration/`.
- Naming convention: `V{version}__{description}.sql` (e.g., `V001__create_loan_products.sql`).
- Flyway runs automatically on application startup via Spring Boot auto-configuration.
- Seed data is delivered as a separate set of migrations:
  `V900__seed_loan_products.sql`, `V901__seed_customers.sql`, etc.
  (high version numbers to keep seed data after schema migrations).
- Repeatable migrations (`R__`) are used for database functions or views if needed.

## Consequences

### Positive

- Schema is fully version-controlled alongside application code.
- `./gradlew bootRun` on a fresh database automatically creates all tables and seed data.
- Plain SQL migrations are reviewable in PRs and auditable.
- Flyway's checksum validation catches accidental edits to applied migrations.

### Negative

- Cannot easily "undo" a migration in Community Edition (no `flyway:undo`). Mitigation:
  write forward-only migrations. If a mistake is made, write a corrective migration.
- SQL migrations are PostgreSQL-specific. If we ever switch databases, migrations need
  rewriting. Acceptable given [ADR-0002](0002-postgresql-as-database.md).

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **Liquibase** | More powerful (XML/YAML/JSON changelogs, rollback support), but also more complex. Flyway's simplicity (plain SQL files) is a better fit for a case study where reviewability matters. |
| **JPA `hibernate.hbm2ddl.auto=update`** | Dangerous. Hibernate's auto-DDL is non-deterministic, cannot handle data migrations, and is explicitly warned against for production use. |
| **Manual SQL scripts** | No tracking of applied migrations. Risk of double-applying or missing migrations on different environments. |
