package com.pranavi.vps.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Incoming request body for POST /api/v1/invoices.
 *
 * A DTO (Data Transfer Object) is deliberately separate from the Invoice entity so the
 * API contract is decoupled from the database schema — callers can't set id, status,
 * version, or timestamps. Bean Validation annotations reject bad input before it reaches
 * our business logic.
 */
public record CreateInvoiceRequest(

    @NotBlank(message = "vendorId is required")
    String vendorId,

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    BigDecimal amount,

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO code")
    String currency
) {}
