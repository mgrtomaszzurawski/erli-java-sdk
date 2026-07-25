package io.github.mgrtomaszzurawski.erli.domain.orders;

import java.util.Optional;

/**
 * The person who placed the order, together with the addresses they gave.
 *
 * <p><strong>Personal data.</strong> The e-mail address Erli returns is usually a per-order proxy
 * rather than the buyer's private one, but it is still personal data, so {@link #toString()} is
 * redacted here and on both address records.
 *
 * @param email           the buyer's (usually proxied) e-mail address
 * @param deliveryAddress where the order is to be delivered
 * @param invoiceAddress  the invoice address, when the buyer asked for an invoice
 */
public record Buyer(
        String email,
        DeliveryAddress deliveryAddress,
        Optional<InvoiceAddress> invoiceAddress) {

    private static final String REDACTED_RENDERING = "Buyer[REDACTED]";

    /** Redacted: this record is buyer personal data and must not reach a log through a string. */
    @Override
    public String toString() {
        return REDACTED_RENDERING;
    }
}
