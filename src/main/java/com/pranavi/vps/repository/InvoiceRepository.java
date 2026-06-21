package com.pranavi.vps.repository;

import com.pranavi.vps.model.Invoice;
import com.pranavi.vps.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA generates the implementation of this interface at runtime.
 * We declare WHAT we want (method names) and Spring writes the SQL.
 *
 *  - findByIdempotencyKey: powers idempotency — before creating, we check if a key exists.
 *  - findByStatus / findByVendorId: query helpers, derived automatically from method names.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findByVendorId(String vendorId);
}
