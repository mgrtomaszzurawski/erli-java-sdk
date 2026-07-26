package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.util.Collection;
import java.util.Optional;

/**
 * Rendering helper that keeps personal data out of {@code toString()} output.
 *
 * <p>Shipping payloads carry the buyer's name, street address, postcode, phone number and e-mail, plus
 * free-text delivery instructions. The binding PII rule targets exactly that material, and a record's
 * generated {@code toString()} would print all of it into any log line, stack trace or debugger
 * snapshot that touches a {@link Parcel}. {@link ShippingParty} and {@link ParcelShipment} therefore
 * render themselves through this helper: presence is disclosed, content is not.
 *
 * <p>Package-private on purpose — it is a rendering detail, not public surface. When the Orders bucket
 * needs the same behaviour for buyer data, this belongs in {@code sdk.core} (see {@code BACKLOG.md}).
 */
final class Redaction {

    /** Stand-in printed instead of a present personal-data value. */
    static final String PRESENT_BUT_REDACTED = "***";
    /** Stand-in printed for an absent value, so presence and absence stay distinguishable. */
    static final String ABSENT = "null";

    private Redaction() {
    }

    /** Render a personal-data optional: {@code ***} when present, {@code null} when empty. */
    static String hide(Optional<String> value) {
        return value.isEmpty() ? ABSENT : PRESENT_BUT_REDACTED;
    }

    /** Render a non-personal optional plainly, using the same absent marker as {@link #hide}. */
    static String show(Optional<?> value) {
        return value.isEmpty() ? ABSENT : String.valueOf(value.get());
    }

    /**
     * Render a collection of personal-data values as a count only — enough to tell "none" from "three"
     * without disclosing any element.
     */
    static String hideCount(Collection<?> values) {
        return values == null ? ABSENT : "[" + values.size() + " " + PRESENT_BUT_REDACTED + "]";
    }
}
