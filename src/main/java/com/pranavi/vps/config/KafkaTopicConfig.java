package com.pranavi.vps.config;

import com.pranavi.vps.kafka.PaymentEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topic as a bean. Spring Kafka's admin client creates it on startup
 * if it doesn't exist. 3 partitions lets us parallelize across consumers while preserving
 * per-invoice ordering (same invoiceId key -> same partition).
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PaymentEventProducer.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
