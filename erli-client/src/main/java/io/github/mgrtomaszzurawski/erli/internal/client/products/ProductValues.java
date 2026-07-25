package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Small conversions shared by the products mappers: money, optionals and null-tolerant list handling.
 * Internal: never exported.
 */
final class ProductValues {

    /** Erli quotes every product price as an integer number of grosze (1/100 PLN). */
    private static final int GROSZE_PER_ZLOTY_SCALE = 2;
    private static final String MARKETPLACE_CURRENCY_CODE = "PLN";
    private static final BigDecimal GROSZE_PER_ZLOTY = BigDecimal.valueOf(100);

    private ProductValues() {
    }

    /** A grosze amount from the wire as PLN {@link Money}. */
    static Money toMoney(Integer grosze) {
        return Money.of(
                BigDecimal.valueOf(grosze.longValue(), GROSZE_PER_ZLOTY_SCALE),
                Currency.getInstance(MARKETPLACE_CURRENCY_CODE));
    }

    /** An optional grosze amount from the wire as PLN {@link Money}. */
    static Optional<Money> toOptionalMoney(Integer grosze) {
        return Optional.ofNullable(grosze).map(ProductValues::toMoney);
    }

    /**
     * A {@link Money} amount back to the integer grosze the API expects. Rejects sub-grosz precision
     * rather than rounding it away silently — a price the caller cannot express exactly is a mistake
     * worth surfacing at the call site, not a penny quietly lost.
     */
    static Integer toGrosze(Money money) {
        BigDecimal grosze = money.amount().multiply(GROSZE_PER_ZLOTY);
        try {
            return grosze.intValueExact();
        } catch (ArithmeticException notWholeGrosze) {
            throw new IllegalArgumentException(
                    "Price " + money.amount() + " " + money.currency().getCurrencyCode()
                            + " cannot be expressed as a whole number of grosze", notWholeGrosze);
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
