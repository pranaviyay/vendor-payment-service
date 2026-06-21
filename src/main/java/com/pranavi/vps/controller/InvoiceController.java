package com.pranavi.vps.controller;

import com.pranavi.vps.dto.CreateInvoiceRequest;
import com.pranavi.vps.dto.InvoiceResponse;
import com.pranavi.vps.model.Invoice;
import com.pranavi.vps.model.InvoiceStatus;
import com.pranavi.vps.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API surface.
 *
 *  POST /api/v1/invoices            create (requires Idempotency-Key header)
 *  GET  /api/v1/invoices/{id}       fetch one
 *  GET  /api/v1/invoices            list (optional ?status= or ?vendorId=)
 *
 * The controller is THIN: it validates input, delegates to the service, and shapes the
 * HTTP response. No business logic lives here.
 */
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @Valid @RequestBody CreateInvoiceRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        Invoice invoice = invoiceService.createInvoice(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceResponse.from(invoice));
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return InvoiceResponse.from(invoiceService.getInvoice(id));
    }

    @GetMapping
    public List<InvoiceResponse> list(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String vendorId) {
        return invoiceService.listInvoices(status, vendorId)
                .stream().map(InvoiceResponse::from).toList();
    }
}
