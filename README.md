# Vendor Payment Processing Service

An event-driven microservice that ingests vendor invoices over REST and processes them
through an asynchronous payment lifecycle using Kafka, with idempotency guarantees that
prevent duplicate payments.

## Stack
Java 21 · Spring Boot 3.3 · Apache Kafka · PostgreSQL · Docker · JUnit 5

---

## Architecture

```
                  POST /api/v1/invoices
                          |
                          v
              +------------------------+
              |   InvoiceController    |  (thin: validate + delegate)
              +-----------+------------+
                          |
                          v
              +------------------------+        +---------------------+
              |     InvoiceService     |------->|     PostgreSQL      |
              | (idempotency + create) |        |  invoices table     |
              +-----------+------------+        +---------------------+
                          |
                          | publish PaymentEvent (key = invoiceId)
                          v
              +------------------------+
              |   Kafka: payment-events|
              +-----------+------------+
                          |  consume (ordered per invoice)
                          v
              +------------------------+
              | PaymentEventConsumer   |
              +-----------+------------+
                          |
                          v
              +----------------------------+
              | InvoiceProcessingService   |  validate -> approve -> pay
              | (guarded state machine)    |  (or -> reject)
              +----------------------------+
```

The HTTP request returns as soon as the invoice is persisted and an event is published.
The lifecycle progression happens asynchronously in the Kafka consumer, off the request
thread.

## Payment lifecycle (state machine)

```
SUBMITTED ──> VALIDATED ──> APPROVED ──> PAID
    │             │
    └──> REJECTED └──> REJECTED
```

PAID and REJECTED are terminal. Every status change goes through a single guarded
`transition()` method that consults `InvoiceStatus.canTransitionTo(...)`; illegal moves
throw `IllegalStateTransitionException`.

---

## Key design decisions

**Idempotency via a unique DB constraint.** Every create requires an `Idempotency-Key`
header. The `invoices` table has a UNIQUE constraint on it. If two identical requests race,
one wins the INSERT and the other catches the constraint violation and returns the existing
invoice — so a retried or duplicated request never creates a second payment.

**`invoiceId` as the Kafka partition key.** Kafka guarantees ordering within a partition.
Keying by `invoiceId` routes all events for one invoice to the same partition, so its
events are always processed in order even with multiple consumers.

**Events carry only the id, not the invoice.** The consumer re-reads current state from the
DB (the source of truth), avoiding action on stale data baked into an old message.

**`BigDecimal` for money, never `double`.** Floating point introduces rounding errors that
are unacceptable for currency.

**Thin controller, logic in services.** Keeps the HTTP layer swappable and the business
rules unit-testable without a web server.

---

## Running it

```bash
# 1. Start Postgres + Kafka
docker compose up -d

# 2. Run the service
mvn spring-boot:run

# 3. Create an invoice (note the required Idempotency-Key header)
curl -X POST http://localhost:8080/api/v1/invoices \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: abc-123" \
  -d '{"vendorId":"VENDOR-001","amount":250.00,"currency":"USD"}'

# 4. Fetch it — status will have progressed to PAID asynchronously
curl http://localhost:8080/api/v1/invoices/<id>

# 5. Send the SAME Idempotency-Key again — returns the same invoice, no duplicate
```

## Testing

```bash
mvn test
```

- `InvoiceStatusTest` — pure unit tests of the state-machine rules.
- `InvoiceServiceTest` — idempotency (same key → one row), full processing to PAID,
  large-amount rejection, and illegal-transition rejection, over an in-memory H2 database.

---

## Possible next steps (deliberately out of scope)

Auth (JWT), a dead-letter topic with retry/backoff for failed events, Flyway migrations
instead of `ddl-auto`, observability (Micrometer + Prometheus), and idempotency-key
expiry. Knowing what was deferred — and why — is itself part of the design.
