package com.pranavi.vps.kafka;

import java.util.UUID;

/**
 * The message we publish to Kafka whenever an invoice needs asynchronous processing.
 *
 * It carries only the invoiceId (not the whole invoice) — the consumer re-reads the
 * current state from the DB, which is the source of truth. This avoids acting on stale
 * data embedded in an old message.
 */
public record PaymentEvent(
    UUID invoiceId,
    String eventType
) {
    public static PaymentEvent forInvoice(UUID invoiceId) {
        return new PaymentEvent(invoiceId, "INVOICE_SUBMITTED");
    }
}
