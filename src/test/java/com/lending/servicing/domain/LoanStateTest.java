package com.lending.servicing.domain;

import com.lending.servicing.domain.model.LoanState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LoanStateTest {

    @Test
    void valid_disburse() {
        assertEquals(LoanState.ACTIVE, LoanState.PENDING_DISBURSEMENT.transitionTo(LoanState.ACTIVE));
    }

    @Test
    void valid_overdue() {
        assertEquals(LoanState.OVERDUE, LoanState.ACTIVE.transitionTo(LoanState.OVERDUE));
    }

    @Test
    void valid_closeFromActive() {
        assertEquals(LoanState.CLOSED, LoanState.ACTIVE.transitionTo(LoanState.CLOSED));
    }

    @Test
    void valid_cureOverdue() {
        assertEquals(LoanState.ACTIVE, LoanState.OVERDUE.transitionTo(LoanState.ACTIVE));
    }

    @Test
    void valid_closeFromOverdue() {
        assertEquals(LoanState.CLOSED, LoanState.OVERDUE.transitionTo(LoanState.CLOSED));
    }

    @Test
    void valid_default() {
        assertEquals(LoanState.DEFAULTED, LoanState.OVERDUE.transitionTo(LoanState.DEFAULTED));
    }

    @Test
    void valid_writeOff() {
        assertEquals(LoanState.WRITTEN_OFF, LoanState.DEFAULTED.transitionTo(LoanState.WRITTEN_OFF));
    }

    @Test
    void invalid_skipDisbursement() {
        assertThrows(LoanState.IllegalLoanStateTransitionException.class,
                () -> LoanState.PENDING_DISBURSEMENT.transitionTo(LoanState.OVERDUE));
    }

    @Test
    void invalid_reopenClosed() {
        assertThrows(LoanState.IllegalLoanStateTransitionException.class,
                () -> LoanState.CLOSED.transitionTo(LoanState.ACTIVE));
    }

    @Test
    void invalid_closeWrittenOff() {
        assertThrows(LoanState.IllegalLoanStateTransitionException.class,
                () -> LoanState.WRITTEN_OFF.transitionTo(LoanState.CLOSED));
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void exhaustive_invalidPairs(LoanState from, LoanState to) {
        assertThrows(LoanState.IllegalLoanStateTransitionException.class,
                () -> from.transitionTo(to));
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(LoanState.PENDING_DISBURSEMENT, LoanState.OVERDUE),
                Arguments.of(LoanState.PENDING_DISBURSEMENT, LoanState.CLOSED),
                Arguments.of(LoanState.PENDING_DISBURSEMENT, LoanState.DEFAULTED),
                Arguments.of(LoanState.PENDING_DISBURSEMENT, LoanState.WRITTEN_OFF),
                Arguments.of(LoanState.ACTIVE, LoanState.PENDING_DISBURSEMENT),
                Arguments.of(LoanState.ACTIVE, LoanState.DEFAULTED),
                Arguments.of(LoanState.ACTIVE, LoanState.WRITTEN_OFF),
                Arguments.of(LoanState.OVERDUE, LoanState.PENDING_DISBURSEMENT),
                Arguments.of(LoanState.OVERDUE, LoanState.WRITTEN_OFF),
                Arguments.of(LoanState.CLOSED, LoanState.PENDING_DISBURSEMENT),
                Arguments.of(LoanState.CLOSED, LoanState.ACTIVE),
                Arguments.of(LoanState.CLOSED, LoanState.OVERDUE),
                Arguments.of(LoanState.CLOSED, LoanState.DEFAULTED),
                Arguments.of(LoanState.CLOSED, LoanState.WRITTEN_OFF),
                Arguments.of(LoanState.DEFAULTED, LoanState.PENDING_DISBURSEMENT),
                Arguments.of(LoanState.DEFAULTED, LoanState.ACTIVE),
                Arguments.of(LoanState.DEFAULTED, LoanState.OVERDUE),
                Arguments.of(LoanState.DEFAULTED, LoanState.CLOSED),
                Arguments.of(LoanState.WRITTEN_OFF, LoanState.PENDING_DISBURSEMENT),
                Arguments.of(LoanState.WRITTEN_OFF, LoanState.ACTIVE),
                Arguments.of(LoanState.WRITTEN_OFF, LoanState.OVERDUE),
                Arguments.of(LoanState.WRITTEN_OFF, LoanState.CLOSED),
                Arguments.of(LoanState.WRITTEN_OFF, LoanState.DEFAULTED)
        );
    }

    @Test
    void terminalStates() {
        assertTrue(LoanState.CLOSED.isTerminal());
        assertTrue(LoanState.WRITTEN_OFF.isTerminal());
        assertFalse(LoanState.ACTIVE.isTerminal());
        assertFalse(LoanState.OVERDUE.isTerminal());
        assertFalse(LoanState.PENDING_DISBURSEMENT.isTerminal());
        assertFalse(LoanState.DEFAULTED.isTerminal());
    }

    @Test
    void activeStates() {
        assertTrue(LoanState.ACTIVE.isActive());
        assertTrue(LoanState.OVERDUE.isActive());
        assertFalse(LoanState.CLOSED.isActive());
        assertFalse(LoanState.PENDING_DISBURSEMENT.isActive());
    }
}
