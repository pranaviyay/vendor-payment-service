package com.pranavi.vps.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes PaymentEvents to the `payment-events` topic.
 *
 * KEY DESIGN POINT: we use invoiceId as the Kafka message KEY. Kafka routes all messages
 * with the same key to the same partition, and a partition is consumed in order. So all
 * events for one invoice are processed in the order they were produced — no race where
 * "approve" overtakes "validate" for the same invoice.
 */
@Component
public class PaymentEventProducer {

    public static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    // Constructor injection: Spring sees this class needs a KafkaTemplate bean and
    // supplies the one it auto-configured. No `new` anywhere.
    public PaymentEventProducer(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PaymentEvent event) {
        // 2nd arg is the partition key (invoiceId), 3rd is the payload.
        kafkaTemplate.send(TOPIC, event.invoiceId().toString(), event);
    }
}
