package com.pranavi.vps.exception;

import java.util.UUID;

/** Thrown when an invoice id doesn't exist. */
public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(UUID id) {
        super("Invoice not found: " + id);
    }
}
