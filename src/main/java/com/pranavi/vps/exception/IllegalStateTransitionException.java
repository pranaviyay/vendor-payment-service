package com.pranavi.vps.exception;

import com.pranavi.vps.model.InvoiceStatus;

/** Thrown when code attempts an illegal state-machine transition. */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(InvoiceStatus from, InvoiceStatus to) {
        super("Illegal transition: " + from + " -> " + to);
    }
}
