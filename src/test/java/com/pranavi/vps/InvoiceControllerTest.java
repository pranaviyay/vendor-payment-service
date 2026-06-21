package com.pranavi.vps;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.pranavi.vps.controller.InvoiceController;
import com.pranavi.vps.exception.GlobalExceptionHandler;
import com.pranavi.vps.exception.InvoiceNotFoundException;
import com.pranavi.vps.model.Invoice;
import com.pranavi.vps.model.InvoiceStatus;
import com.pranavi.vps.service.InvoiceService;

/**
 * Web-layer tests using standalone MockMvc. The service is a Mockito mock, so these
 * exercise the controller + global exception handler without a database, Kafka, or
 * a full Spring context.
 */
class InvoiceControllerTest {

    private InvoiceService invoiceService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        invoiceService = mock(InvoiceService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InvoiceController(invoiceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Invoice sampleInvoice(UUID id, InvoiceStatus status) {
        Invoice inv = new Invoice();
        inv.setId(id);
        inv.setVendorId("VENDOR-001");
        inv.setAmount(new BigDecimal("250.00"));
        inv.setCurrency("USD");
        inv.setStatus(status);
        inv.setIdempotencyKey("key-1");
        inv.setCreatedAt(Instant.now());
        inv.setUpdatedAt(Instant.now());
        return inv;
    }

    @Test
    void createInvoice_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(invoiceService.createInvoice(any(), eq("key-1")))
                .thenReturn(sampleInvoice(id, InvoiceStatus.SUBMITTED));

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vendorId\":\"VENDOR-001\",\"amount\":250.00,\"currency\":\"USD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void createInvoice_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vendorId\":\"VENDOR-001\",\"amount\":250.00,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createInvoice_invalidAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/invoices")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vendorId\":\"VENDOR-001\",\"amount\":-5,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvoice_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(invoiceService.getInvoice(id)).thenReturn(sampleInvoice(id, InvoiceStatus.PAID));

        mockMvc.perform(get("/api/v1/invoices/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void getInvoice_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(invoiceService.getInvoice(id)).thenThrow(new InvoiceNotFoundException(id));

        mockMvc.perform(get("/api/v1/invoices/" + id))
                .andExpect(status().isNotFound());
    }
}