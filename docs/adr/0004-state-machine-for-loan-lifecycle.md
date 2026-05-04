# ADR-0004: Hand-Rolled Enum-Based State Machine for Loan Lifecycle

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

A `LoanAccount` transitions through a defined set of states:
`PENDING_DISBURSEMENT → ACTIVE → OVERDUE → DEFAULTED → WRITTEN_OFF → CLOSED`
(see [02-domain-model.md](../02-domain-model.md) for the full state diagram).

State transitions must be:

1. **Explicit** — the valid transitions are enumerable and auditable.
2. **Enforced** — illegal transitions (e.g., `CLOSED → ACTIVE`) must be rejected at
   the domain layer, not rely on controller validation.
3. **Testable** — transition logic must be testable without Spring context.
4. **Readable** — a new developer should understand all valid transitions by reading
   one file.

## Decision

Implement the loan state machine as a **hand-rolled Java enum** with a transition
validation method.

```java
public enum LoanState {
    PENDING_DISBURSEMENT,
    ACTIVE,
    OVERDUE,
    DEFAULTED,
    WRITTEN_OFF,
    CLOSED;

    private static final Map<LoanState, Set<LoanState>> VALID_TRANSITIONS = Map.of(
        PENDING_DISBURSEMENT, Set.of(ACTIVE),
        ACTIVE, Set.of(OVERDUE, CLOSED),
        OVERDUE, Set.of(ACTIVE, CLOSED, DEFAULTED),
        DEFAULTED, Set.of(WRITTEN_OFF),
        WRITTEN_OFF, Set.of(),
        CLOSED, Set.of()
    );

    public LoanState transitionTo(LoanState target) {
        if (!VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target)) {
            throw new IllegalLoanStateTransitionException(this, target);
        }
        return target;
    }
}
```

- The transition map is the single source of truth for valid transitions.
- `LoanAccount.transitionTo(newState)` delegates to the enum, then publishes the
  appropriate domain event.
- Side effects (ledger entries, notifications) are triggered by domain events, not
  inlined in the state machine.

## Consequences

### Positive

- ~30 lines of code. Zero dependencies. Fully unit-testable.
- The transition map is a complete, readable specification of the lifecycle.
- Adding a new state (e.g., `RESTRUCTURED`) is a one-line addition to the map + enum constant.
- No framework lock-in.

### Negative

- No built-in support for guards, actions, or hierarchical states. If the lifecycle
  becomes significantly more complex (10+ states, conditional transitions), this approach
  may need to be upgraded to a more formal state machine.
- Transition side effects are handled by event listeners, which adds indirection. This is
  intentional — side effects should not be coupled to the state machine — but it requires
  developers to trace event flow.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **Spring Statemachine** | Full-featured, supports guards, actions, regions, and persistence. But it is heavyweight for 6 states: requires configuring `StateMachineFactory`, `StateMachineListener`, and persistence adapters. The learning curve and configuration overhead are not justified for this domain complexity. Would revisit if the lifecycle exceeds ~10 states with conditional guards. |
| **Status field with ad-hoc if/else checks** | Scatters transition logic across service methods. No single place to see all valid transitions. Leads to bugs when a new developer adds a transition without checking all call sites. |
| **Workflow engine (Camunda, Temporal)** | Extreme overkill for a linear-with-branches lifecycle. Adds operational infrastructure (process engine, database tables, admin UI). Justified for multi-party approval workflows with human tasks, not for a simple state machine. |
| **Database-level state constraint (`CHECK` on enum values)** | Enforces valid *values* but not valid *transitions*. The database cannot reject `CLOSED → ACTIVE` because it only sees the new value, not the old one. Must be enforced in application code. |
