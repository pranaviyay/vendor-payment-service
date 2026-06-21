package com.pranavi.vps.kafka;

import com.pranavi.vps.service.InvoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to `payment-events` and drives the invoice forward through its lifecycle
 * asynchronously. This is what makes the system event-driven: the HTTP request returns
 * immediately after the invoice is saved + event published; the actual progression
 * (validate -> approve -> pay) happens here, off the request thread.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final InvoiceProcessingService processingService;

    public PaymentEventConsumer(InvoiceProcessingService processingService) {
        this.processingService = processingService;
    }

    @KafkaListener(topics = PaymentEventProducer.TOPIC, groupId = "payment-processor")
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Received event type={} invoiceId={}", event.eventType(), event.invoiceId());
        try {
            processingService.processToCompletion(event.invoiceId());
        } catch (Exception e) {
            // In production you'd route failures to a dead-letter topic and/or retry.
            log.error("Failed to process invoiceId={}: {}", event.invoiceId(), e.getMessage(), e);
        }
    }
}
