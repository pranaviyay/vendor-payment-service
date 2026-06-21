package com.pranavi.vps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Application entry point.
 *
 * @SpringBootApplication bundles three annotations:
 *   - @Configuration       : this class can define beans
 *   - @EnableAutoConfiguration : Spring auto-wires sensible defaults (web server, JPA, Kafka)
 *   - @ComponentScan       : Spring scans this package (and sub-packages) for beans
 *
 * @EnableKafka activates Spring's @KafkaListener machinery so our consumer is detected.
 */
@SpringBootApplication
@EnableKafka
public class VendorPaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendorPaymentServiceApplication.class, args);
    }
}
