package io.github.mgrtomaszzurawski.erli.internal.client.finance;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Conversion between the Erli money wire format and {@link Money}. Every amount in the Finance
 * domain — billing entries, payouts, commissions, campaign costs — is a whole number of
 * <em>grosze</em> (PLN minor units); only {@code Payment.amount} is already expressed in złoty. The
 * domain layer exposes {@link Money} throughout so consumers never have to remember which is which.
 *
 * <p>Shared by the four Finance areas ({@code commissions}, {@code billing}, {@code campaigns},
 * {@code payments}). Internal: never exported.
 */
public final class MinorUnits {

    /** The Erli marketplace settles in Polish złoty; the API carries no currency field. */
    public static final Currency PLN = Currency.getInstance("PLN");

    /**
     * Scale taken from the currency itself rather than a hard-coded 2, matching how the Orders bucket
     * rebuilds {@code Money} (see {@code KNOWN-SERVER-BEHAVIORS.md}). PLN has two minor digits today;
     * deriving it keeps the two buckets consistent if a non-decimal currency ever appears.
     */
    private static final int MINOR_UNIT_SCALE = PLN.getDefaultFractionDigits();

    private MinorUnits() {
    }

    /** Convert a whole number of grosze to {@link Money}, e.g. {@code 1168} to {@code 11.68 PLN}. */
    public static Money fromGrosze(long grosze) {
        return new Money(BigDecimal.valueOf(grosze, MINOR_UNIT_SCALE), PLN);
    }

    /**
     * Convert a grosze amount the API typed as a JSON {@code number} rather than an integer (campaign
     * costs). A fractional grosz is preserved rather than rounded — the caller decides what to do
     * with it.
     */
    public static Money fromGroszeAmount(BigDecimal grosze) {
        Objects.requireNonNull(grosze, "grosze");
        return new Money(grosze.movePointLeft(MINOR_UNIT_SCALE), PLN);
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
     * @param majorUnits  the amount as the API sent it
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
        BigDecimal grosze = money.amount().movePointRight(MINOR_UNIT_SCALE);
        try {
            return grosze.intValueExact();
        } catch (ArithmeticException notWholeGrosze) {
            throw new IllegalArgumentException(
                    fieldName + " must be a whole number of grosze and fit in 32 bits, got "
                            + money.amount().toPlainString(), notWholeGrosze);
        }
    }
}
