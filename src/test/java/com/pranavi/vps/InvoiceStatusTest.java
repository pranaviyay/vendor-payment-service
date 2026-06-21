package com.pranavi.vps;

import com.pranavi.vps.model.InvoiceStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the state-machine rules. No Spring context, no DB — fast and focused.
 * These prove the legal/illegal transition logic directly.
 */
class InvoiceStatusTest {

    @Test
    void submitted_canGoTo_validated_or_rejected() {
        assertThat(InvoiceStatus.SUBMITTED.canTransitionTo(InvoiceStatus.VALIDATED)).isTrue();
        assertThat(InvoiceStatus.SUBMITTED.canTransitionTo(InvoiceStatus.REJECTED)).isTrue();
    }

    @Test
    void submitted_cannot_skip_to_paid() {
        assertThat(InvoiceStatus.SUBMITTED.canTransitionTo(InvoiceStatus.PAID)).isFalse();
        assertThat(InvoiceStatus.SUBMITTED.canTransitionTo(InvoiceStatus.APPROVED)).isFalse();
    }

    @Test
    void validated_canGoTo_approved_or_rejected() {
        assertThat(InvoiceStatus.VALIDATED.canTransitionTo(InvoiceStatus.APPROVED)).isTrue();
        assertThat(InvoiceStatus.VALIDATED.canTransitionTo(InvoiceStatus.REJECTED)).isTrue();
    }

    @Test
    void approved_canGoTo_paid_only() {
        assertThat(InvoiceStatus.APPROVED.canTransitionTo(InvoiceStatus.PAID)).isTrue();
        assertThat(InvoiceStatus.APPROVED.canTransitionTo(InvoiceStatus.REJECTED)).isFalse();
    }

    @Test
    void paid_and_rejected_are_terminal() {
        assertThat(InvoiceStatus.PAID.isTerminal()).isTrue();
        assertThat(InvoiceStatus.REJECTED.isTerminal()).isTrue();
        assertThat(InvoiceStatus.PAID.canTransitionTo(InvoiceStatus.APPROVED)).isFalse();
    }
}
