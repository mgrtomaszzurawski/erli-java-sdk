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

    /** A Polish-złoty amount, the Erli marketplace default currency. */
    public static Money ofPln(String amount) {
        return of(amount, DEFAULT_CURRENCY_CODE);
    }
}
