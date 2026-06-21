package com.pranavi.vps.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pranavi.vps.exception.IllegalStateTransitionException;
import com.pranavi.vps.exception.InvoiceNotFoundException;
import com.pranavi.vps.model.Invoice;
import com.pranavi.vps.model.InvoiceStatus;
import com.pranavi.vps.repository.InvoiceRepository;

/**
 * Advances an invoice through the lifecycle. Called by the Kafka consumer.
 *
 * transition() is the ONLY method that changes status, and it always asks the enum
 * "is this move legal?" first. Centralizing the rule means the state machine can never
 * be bypassed.
 *
 * processToCompletion() runs the business steps: validate -> approve -> pay,
 * with a simple business rule (reject absurdly large amounts at validation).
 */
@Service
public class InvoiceProcessingService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceProcessingService.class);
    private static final BigDecimal MAX_AUTO_APPROVE = new BigDecimal("1000000.00");

    private final InvoiceRepository repository;

    public InvoiceProcessingService(InvoiceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void processToCompletion(UUID invoiceId) {
    Invoice invoice = repository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

    // IDEMPOTENT CONSUMER: if this invoice is already in a terminal state, a redelivered
    // Kafka event is a no-op. Kafka guarantees at-least-once delivery, so the same event
    // CAN arrive twice; we must not re-run the pipeline on an already-finished invoice.
    if (invoice.getStatus().isTerminal()) {
        log.info("Invoice {} already terminal ({}), skipping duplicate event",
                invoiceId, invoice.getStatus());
        return;
    }

    // Only start the pipeline from a fresh SUBMITTED invoice. Any other non-terminal
    // state means processing is already underway; skip rather than double-act.
    if (invoice.getStatus() != InvoiceStatus.SUBMITTED) {
        log.info("Invoice {} not in SUBMITTED state (was {}), skipping", invoiceId, invoice.getStatus());
        return;
    }

    // VALIDATION step
    if (invoice.getAmount().compareTo(MAX_AUTO_APPROVE) > 0) {
        transition(invoice, InvoiceStatus.REJECTED);
        log.info("Invoice {} rejected: amount exceeds auto-approve limit", invoiceId);
        return;
    }
    transition(invoice, InvoiceStatus.VALIDATED);

    // APPROVAL step (auto-approval placeholder — a real AP system gates here on a
    // human approver or a business-rules engine)
    transition(invoice, InvoiceStatus.APPROVED);

    // PAYMENT step
    transition(invoice, InvoiceStatus.PAID);
    log.info("Invoice {} completed: PAID", invoiceId);
}

    /**
     * The single guarded mutation. Throws if the transition is illegal.
     */
    @Transactional
    public Invoice transition(Invoice invoice, InvoiceStatus next) {
        InvoiceStatus current = invoice.getStatus();
        if (!current.canTransitionTo(next)) {
            throw new IllegalStateTransitionException(current, next);
        }
        invoice.setStatus(next);
        return repository.save(invoice);
    }
}
