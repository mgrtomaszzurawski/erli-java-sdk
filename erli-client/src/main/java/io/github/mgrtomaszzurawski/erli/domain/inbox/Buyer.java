package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.Objects;
import java.util.Optional;

/**
 * The person who placed the order, as far as an order event reveals them.
 *
 * <p><strong>Buyer personal data.</strong> {@link #toString()} redacts the e-mail address and defers
 * to the addresses' own redacting {@code toString()}.
 *
 * @param email          the buyer's e-mail address
 * @param deliveryAddress where the order goes
 * @param invoiceAddress  where the invoice goes, when it differs from the delivery address
 */
public record Buyer(String email, DeliveryAddress deliveryAddress, Optional<InvoiceAddress> invoiceAddress) {

    private static final String REDACTED = "<redacted>";

    public Buyer {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(deliveryAddress, "deliveryAddress");
        Objects.requireNonNull(invoiceAddress, "invoiceAddress");
    }

    @Override
    public String toString() {
        return "Buyer[email=" + REDACTED
                + ", deliveryAddress=" + deliveryAddress
                + ", invoiceAddress=" + invoiceAddress.map(Object::toString).orElse("absent")
                + "]";
    }
}
