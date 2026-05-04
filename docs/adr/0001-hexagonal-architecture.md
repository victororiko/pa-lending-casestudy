# ADR-0001: Hexagonal Architecture

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

The lending platform must be maintainable and extensible over 12–18 months by a team
that will grow. The case study brief explicitly evaluates "how the system you produce
could be maintained and extended by a team over time." We need an architecture that:

1. Isolates domain logic from infrastructure concerns (database, HTTP, messaging).
2. Makes domain logic testable without booting Spring or connecting to PostgreSQL.
3. Allows swapping infrastructure adapters (e.g., replacing a stub notification channel
   with a real SMS gateway) without touching business rules.
4. Provides clear boundaries between bounded contexts
   (see [02-domain-model.md](../02-domain-model.md)).

## Decision

Adopt **hexagonal architecture** (ports and adapters) as the structural pattern for
the application.

### Structure per bounded context:

```
com.lending.{context}
  ├── domain/           # Entities, value objects, domain events, domain services
  │   ├── model/
  │   ├── event/
  │   └── service/
  ├── port/
  │   ├── in/           # Inbound ports (use cases) — interfaces
  │   └── out/          # Outbound ports (repositories, external services) — interfaces
  └── adapter/
      ├── in/
      │   └── web/      # REST controllers (driving adapters)
      └── out/
          ├── persistence/  # JPA repositories, entity mappers (driven adapters)
          └── messaging/    # Event publishers (driven adapters)
```

- **Domain layer** contains zero Spring annotations (except `@Component` on domain
  services if needed for DI). No JPA annotations, no Jackson annotations.
- **Port interfaces** define the contract. Inbound ports are implemented by domain
  services. Outbound ports are implemented by adapters.
- **Adapters** are the only place where framework-specific code lives. JPA entities in
  `persistence/` are separate from domain entities — mapped via dedicated mapper classes.

## Consequences

### Positive

- Domain logic is framework-independent and can be unit-tested with plain JUnit + mocks.
- Replacing PostgreSQL with another store means writing new outbound adapters only.
- New inbound adapters (e.g., a CLI, a gRPC endpoint) can be added without modifying
  domain code.
- Bounded context boundaries are physically enforced by package structure.

### Negative

- More boilerplate: separate JPA entities, domain entities, and mappers between them.
  For a case study this is manageable; for a trivial CRUD app it would be over-engineering.
- Developers unfamiliar with the pattern need onboarding on the port/adapter split.
- Some pragmatic shortcuts may be tempting (e.g., using JPA entities directly as domain
  objects). The team must resist these shortcuts to preserve the architecture's benefits.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **Traditional layered architecture** (Controller → Service → Repository) | Tight coupling between domain and persistence. Domain logic bleeds into service classes that depend on Spring Data. Difficult to test without Spring context. |
| **Clean Architecture (Uncle Bob)** | Conceptually identical to hexagonal. We prefer the "ports and adapters" vocabulary because it is more concrete and maps directly to the package structure. |
| **CQRS + Event Sourcing** | Powerful but extreme for a case-study scope. Event sourcing adds operational complexity (event store, projections, eventual consistency). Worth revisiting if the system grows to need full audit replay, but the append-only ledger gives us 80% of the benefit at 20% of the cost. |
| **Modular monolith with Spring Modulith** | Good fit conceptually, but adds a dependency on a relatively new Spring project. The hexagonal layout achieves similar modularity without additional framework coupling. |
