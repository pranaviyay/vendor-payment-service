package com.pranavi.vps.dto;

import com.pranavi.vps.model.Invoice;
import com.pranavi.vps.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outgoing representation of an invoice. Keeps internal fields (version) out of the API,
 * and gives us a stable response shape even if the entity changes.
 */
public record InvoiceResponse(
    UUID id,
    String vendorId,
    BigDecimal amount,
    String currency,
    InvoiceStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static InvoiceResponse from(Invoice i) {
        return new InvoiceResponse(
            i.getId(), i.getVendorId(), i.getAmount(), i.getCurrency(),
            i.getStatus(), i.getCreatedAt(), i.getUpdatedAt()
        );
    }
}
