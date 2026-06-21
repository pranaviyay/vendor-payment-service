package com.pranavi.vps.model;

import java.util.Set;

/**
 * The payment lifecycle, modeled as a state machine.
 *
 * Legal transitions:
 *   SUBMITTED  -> VALIDATED, REJECTED
 *   VALIDATED  -> APPROVED, REJECTED
 *   APPROVED   -> PAID
 *   PAID       -> (terminal, no transitions out)
 *   REJECTED   -> (terminal, no transitions out)
 *
 * Encoding the rules IN the enum keeps the single source of truth next to the states
 * themselves, so no other class can invent an illegal transition.
 */
public enum InvoiceStatus {
    SUBMITTED,
    VALIDATED,
    APPROVED,
    PAID,
    REJECTED;

    /**
     * @return the set of states this state is allowed to move to.
     */
    public Set<InvoiceStatus> allowedNextStates() {
        return switch (this) {
            case SUBMITTED -> Set.of(VALIDATED, REJECTED);
            case VALIDATED -> Set.of(APPROVED, REJECTED);
            case APPROVED  -> Set.of(PAID);
            case PAID, REJECTED -> Set.of(); // terminal
        };
    }

    /** @return true if moving from `this` state to `next` is a legal transition. */
    public boolean canTransitionTo(InvoiceStatus next) {
        return allowedNextStates().contains(next);
    }

    public boolean isTerminal() {
        return allowedNextStates().isEmpty();
    }
}
