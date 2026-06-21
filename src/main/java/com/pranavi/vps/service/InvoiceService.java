package com.pranavi.vps.service;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pranavi.vps.dto.CreateInvoiceRequest;
import com.pranavi.vps.exception.InvoiceNotFoundException;
import com.pranavi.vps.kafka.PaymentEvent;
import com.pranavi.vps.kafka.PaymentEventProducer;
import com.pranavi.vps.model.Invoice;
import com.pranavi.vps.model.InvoiceStatus;
import com.pranavi.vps.repository.InvoiceRepository;

/**
 * Handles invoice creation and reads.
 *
 * IDEMPOTENCY (the most important behaviour for a payments system):
 *   1. Fast path: if an invoice already exists for this idempotency key, return it.
 *   2. Otherwise create it.
 *   3. Race safety: if two requests with the same key arrive simultaneously, both may
 *      pass the check in step 1. One wins the INSERT; the other hits the UNIQUE constraint
 *      and throws DataIntegrityViolationException, which we CATCH and resolve by returning
 *      the now-existing invoice. Net result: exactly one invoice, never a double payment.
 */
@Service
public class InvoiceService {

    private final InvoiceRepository repository;
    private final PaymentEventProducer producer;

    public InvoiceService(InvoiceRepository repository, PaymentEventProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Transactional
    public Invoice createInvoice(CreateInvoiceRequest req, String idempotencyKey) {
        // Step 1: fast path
        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Step 2: create
        Invoice invoice = new Invoice();
        invoice.setVendorId(req.vendorId());
        invoice.setAmount(req.amount());
        invoice.setCurrency(req.currency());
        invoice.setStatus(InvoiceStatus.SUBMITTED);
        invoice.setIdempotencyKey(idempotencyKey);

        Invoice saved;
        try {
            saved = repository.saveAndFlush(invoice); // flush now so the constraint fires here
        } catch (DataIntegrityViolationException e) {
            // Step 3: lost the race — another request inserted the same key first.
            return repository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e); // unexpected, rethrow
        }

        // Kick off async processing
        producer.publish(PaymentEvent.forInvoice(saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(UUID id) {
        return repository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Invoice> listInvoices(InvoiceStatus status, String vendorId) {
        if (status != null) return repository.findByStatus(status);
        if (vendorId != null) return repository.findByVendorId(vendorId);
        return repository.findAll();
    }
}
