package com.pranavi.vps;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.pranavi.vps.dto.CreateInvoiceRequest;
import com.pranavi.vps.exception.IllegalStateTransitionException;
import com.pranavi.vps.kafka.PaymentEventProducer;
import com.pranavi.vps.model.Invoice;
import com.pranavi.vps.model.InvoiceStatus;
import com.pranavi.vps.repository.InvoiceRepository;
import com.pranavi.vps.service.InvoiceProcessingService;
import com.pranavi.vps.service.InvoiceService;

/**
 * Integration-style tests over a real (in-memory H2) database, but with the Kafka
 * producer mocked out so we don't need a broker. Verifies idempotency + the full
 * processing pipeline + illegal-transition rejection.
 */
@DataJpaTest
@Import({InvoiceService.class, InvoiceProcessingService.class, InvoiceServiceTest.MockProducerConfig.class})
class InvoiceServiceTest {

    @TestConfiguration
    static class MockProducerConfig {
        @Bean
        PaymentEventProducer paymentEventProducer() {
            return mock(PaymentEventProducer.class); // no real Kafka needed
        }
    }

    @Autowired InvoiceService invoiceService;
    @Autowired InvoiceProcessingService processingService;
    @Autowired InvoiceRepository repository;

    private CreateInvoiceRequest sampleRequest() {
        return new CreateInvoiceRequest("VENDOR-001", new BigDecimal("250.00"), "USD");
    }

    @Test
    void createInvoice_persistsWithSubmittedStatus() {
        Invoice inv = invoiceService.createInvoice(sampleRequest(), "key-1");
        assertThat(inv.getId()).isNotNull();
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
    }

    @Test
    void sameIdempotencyKey_returnsSameInvoice_noDuplicate() {
        Invoice first = invoiceService.createInvoice(sampleRequest(), "key-dup");
        Invoice second = invoiceService.createInvoice(sampleRequest(), "key-dup");

        assertThat(second.getId()).isEqualTo(first.getId());      // same invoice
        assertThat(repository.findAll()).hasSize(1);              // only ONE row
    }

    @Test
    void processToCompletion_movesInvoiceToPaid() {
        Invoice inv = invoiceService.createInvoice(sampleRequest(), "key-pay");
        processingService.processToCompletion(inv.getId());

        Invoice reloaded = repository.findById(inv.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void largeAmount_isRejected() {
        var bigReq = new CreateInvoiceRequest("VENDOR-001", new BigDecimal("5000000.00"), "USD");
        Invoice inv = invoiceService.createInvoice(bigReq, "key-big");
        processingService.processToCompletion(inv.getId());

        Invoice reloaded = repository.findById(inv.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(InvoiceStatus.REJECTED);
    }

    @Test
    void illegalTransition_throws() {
        Invoice inv = invoiceService.createInvoice(sampleRequest(), "key-illegal");
        // SUBMITTED -> PAID is illegal (must go through VALIDATED, APPROVED)
        assertThatThrownBy(() -> processingService.transition(inv, InvoiceStatus.PAID))
                .isInstanceOf(IllegalStateTransitionException.class);
    }
    @Test
    void duplicateEvent_isIdempotent_noDoublePayment() {
    Invoice inv = invoiceService.createInvoice(sampleRequest(), "key-dup-event");

    // First processing: SUBMITTED -> ... -> PAID
    processingService.processToCompletion(inv.getId());
    Invoice afterFirst = repository.findById(inv.getId()).orElseThrow();
    assertThat(afterFirst.getStatus()).isEqualTo(InvoiceStatus.PAID);

    // Simulate Kafka redelivering the SAME event — must be a clean no-op, not an error
    processingService.processToCompletion(inv.getId());
    Invoice afterSecond = repository.findById(inv.getId()).orElseThrow();
    assertThat(afterSecond.getStatus()).isEqualTo(InvoiceStatus.PAID);
}
}
