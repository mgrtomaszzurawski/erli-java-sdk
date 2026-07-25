package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Small conversions shared by the products mappers: money, optionals and null-tolerant list handling.
 * Internal: never exported.
 */
final class ProductValues {

    /** Erli quotes every product price as an integer number of minor units ("w groszach"). */
    private static final String MARKETPLACE_CURRENCY_CODE = "PLN";
    private static final Currency MARKETPLACE_CURRENCY = Currency.getInstance(MARKETPLACE_CURRENCY_CODE);

    private ProductValues() {
    }

    /** A minor-units amount from the wire as {@link Money}, via the core helper. */
    static Money toMoney(Integer minorUnits) {
        return Money.ofMinorUnits(minorUnits.longValue(), MARKETPLACE_CURRENCY);
    }

    /** An optional minor-units amount from the wire as {@link Money}. */
    static Optional<Money> toOptionalMoney(Integer minorUnits) {
        return Optional.ofNullable(minorUnits).map(ProductValues::toMoney);
    }

    /**
     * A {@link Money} amount back to the integer minor units the API expects — the inverse of
     * {@link Money#ofMinorUnits(long, java.util.Currency)}, which core does not yet provide (see
     * BACKLOG: a {@code Money.toMinorUnits()} counterpart belongs next to it).
     *
     * <p>Scales by the currency's own fraction digits rather than a hard-coded 100, matching how core
     * reads the value back. Rejects sub-minor-unit precision instead of rounding it away silently: a
     * price the caller cannot express exactly is a mistake worth surfacing at the call site, not a
     * penny quietly lost.
     */
    static Integer toMinorUnits(Money money) {
        int scale = Math.max(money.currency().getDefaultFractionDigits(), 0);
        try {
            return money.amount().movePointRight(scale).intValueExact();
        } catch (ArithmeticException notWholeMinorUnits) {
            throw new IllegalArgumentException(
                    "Price " + money.amount() + " " + money.currency().getCurrencyCode()
                            + " cannot be expressed as a whole number of minor units",
                    notWholeMinorUnits);
        }
    }

    /** An empty list for a missing array, so domain records never expose {@code null}. */
    static <T> List<T> orEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    /** Map each element of a possibly-missing list, skipping {@code null} entries. */
    static <S, T> List<T> mapEach(List<S> source, Function<S, T> mapper) {
        return orEmpty(source).stream().filter(java.util.Objects::nonNull).map(mapper).toList();
    }

    /** {@link Optional#ofNullable} narrowed through a mapper, for nested objects. */
    static <S, T> Optional<T> mapOptional(S source, Function<S, T> mapper) {
        return Optional.ofNullable(source).map(mapper);
    }
}
