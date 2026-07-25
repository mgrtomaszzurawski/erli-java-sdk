package io.github.mgrtomaszzurawski.erli.core.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * A monetary amount in a specific currency. Uses {@link BigDecimal} to avoid binary rounding; the
 * scale is preserved as supplied by the API. Erli's marketplace currency is typically {@code PLN}.
 *
 * @param amount   the value (never null)
 * @param currency the ISO-4217 currency (never null)
 */
public record Money(BigDecimal amount, Currency currency) {

    private static final String DEFAULT_CURRENCY_CODE = "PLN";

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    /**
     * Build from integer minor units at the currency's own scale — Erli sends money amounts as
     * integers "w groszach" (e.g. {@code 1299} PLN → {@code 12.99} zł). Uses the currency's default
     * fraction digits rather than a hard-coded 100, so non-2-decimal currencies stay correct. Owned by
     * core so Orders/Products/Finance stop each re-deriving this.
     *
     * @param minorUnits the amount in the currency's smallest unit
     * @param currency   the currency (its {@code defaultFractionDigits} sets the scale)
     */
    public static Money ofMinorUnits(long minorUnits, Currency currency) {
        Objects.requireNonNull(currency, "currency");
        int scale = Math.max(currency.getDefaultFractionDigits(), 0);
        return new Money(BigDecimal.valueOf(minorUnits, scale), currency);
    }

    /** {@link #ofMinorUnits(long, Currency)} keyed by ISO-4217 code (e.g. {@code "PLN"}). */
    public static Money ofMinorUnits(long minorUnits, String currencyCode) {
        return ofMinorUnits(minorUnits, Currency.getInstance(currencyCode));
    }

    /** A Polish-złoty amount, the Erli marketplace default currency. */
    public static Money ofPln(String amount) {
        return of(amount, DEFAULT_CURRENCY_CODE);
    }
}
