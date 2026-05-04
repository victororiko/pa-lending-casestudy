package com.lending.servicing.domain.model;

import java.util.Map;
import java.util.Set;

public enum LoanState {
    PENDING_DISBURSEMENT,
    ACTIVE,
    OVERDUE,
    CLOSED,
    DEFAULTED,
    WRITTEN_OFF;

    private static final Map<LoanState, Set<LoanState>> VALID_TRANSITIONS = Map.of(
            PENDING_DISBURSEMENT, Set.of(ACTIVE),
            ACTIVE, Set.of(OVERDUE, CLOSED),
            OVERDUE, Set.of(ACTIVE, CLOSED, DEFAULTED),
            DEFAULTED, Set.of(WRITTEN_OFF),
            CLOSED, Set.of(),
            WRITTEN_OFF, Set.of()
    );

    public boolean canTransitionTo(LoanState target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public LoanState transitionTo(LoanState target) {
        if (!canTransitionTo(target)) {
            throw new IllegalLoanStateTransitionException(this, target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == CLOSED || this == WRITTEN_OFF;
    }

    public boolean isActive() {
        return this == ACTIVE || this == OVERDUE;
    }

    public static class IllegalLoanStateTransitionException extends IllegalStateException {
        public IllegalLoanStateTransitionException(LoanState from, LoanState to) {
            super("Invalid loan state transition from " + from + " to " + to);
        }
    }
}
