package io.github.mgrtomaszzurawski.erli.internal.client.finance;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Finance-specific money conversion, layered on core's {@link Money#ofMinorUnits(long, Currency)}.
 *
 * <p>Core owns the minor-units arithmetic (CORE-8). What stays here is the part that is specific to
 * this domain: which Erli field is in grosze and which is already in złoty, the wire currency a
 * transaction line carries, and the outbound direction with its validation. Internal: never exported.
 */
public final class MinorUnits {

    /** The Erli marketplace settles in Polish złoty; most Finance payloads carry no currency field. */
    public static final Currency PLN = Currency.getInstance("PLN");

    private MinorUnits() {
    }

    /** Convert a whole number of grosze to {@link Money}, e.g. {@code 1168} to {@code 11.68 PLN}. */
    public static Money fromGrosze(long grosze) {
        return Money.ofMinorUnits(grosze, PLN);
    }

    /**
     * Convert a grosze amount the API typed as a JSON {@code number} rather than an integer (campaign
     * costs). A fractional grosz is preserved rather than rounded — the caller decides what to do
     * with it, so this cannot go through the integer-only core helper.
     */
    public static Money fromGroszeAmount(BigDecimal grosze) {
        Objects.requireNonNull(grosze, "grosze");
        return new Money(grosze.movePointLeft(PLN.getDefaultFractionDigits()), PLN);
    }

    /** Convert an amount already expressed in złoty to {@link Money} (used by {@code Payment.amount}). */
    public static Money fromMajorUnits(BigDecimal zloty) {
        Objects.requireNonNull(zloty, "zloty");
        return new Money(zloty, PLN);
    }

    /**
     * Convert an amount in major units that carries its own currency on the wire. Transaction lines
     * are the one place the API states a currency per amount, so they must not be stamped PLN — the
     * SDK exposes a German marketplace too, and a EUR line summed as PLN is silently wrong.
     *
     * @param majorUnits   the amount as the API sent it
     * @param currencyCode the ISO-4217 code from the same payload, or {@code null} to fall back to PLN
     */
    public static Money fromMajorUnits(BigDecimal majorUnits, String currencyCode) {
        Objects.requireNonNull(majorUnits, "majorUnits");
        return new Money(majorUnits, toCurrency(currencyCode));
    }

    /** Resolve a wire currency code, defaulting to PLN when the API omits or misreports it. */
    private static Currency toCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return PLN;
        }
        try {
            return Currency.getInstance(currencyCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknownCurrency) {
            // An unrecognised code must not fail the whole page; PLN is the marketplace default.
            return PLN;
        }
    }

    /**
     * Convert {@link Money} back to whole grosze for the wire.
     *
     * @throws IllegalArgumentException if the currency is not PLN, or the amount carries a fraction
     *                                  of a grosz that would be silently lost
     */
    public static int toGrosze(Money money, String fieldName) {
        Objects.requireNonNull(money, fieldName);
        if (!PLN.equals(money.currency())) {
            throw new IllegalArgumentException(
                    fieldName + " must be in " + PLN.getCurrencyCode()
                            + "; the Erli marketplace settles only in Polish złoty, got "
                            + money.currency().getCurrencyCode());
        }
        BigDecimal grosze = money.amount().movePointRight(PLN.getDefaultFractionDigits());
        try {
            return grosze.intValueExact();
        } catch (ArithmeticException notWholeGrosze) {
            throw new IllegalArgumentException(
                    fieldName + " must be a whole number of grosze and fit in 32 bits, got "
                            + money.amount().toPlainString(), notWholeGrosze);
        }
    }
}
