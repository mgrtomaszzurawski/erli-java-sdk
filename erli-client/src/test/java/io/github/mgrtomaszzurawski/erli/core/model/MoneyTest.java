package io.github.mgrtomaszzurawski.erli.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency PLN = Currency.getInstance("PLN");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Test
    void ofMinorUnitsUsesTheCurrencyScale() {
        Money money = Money.ofMinorUnits(1299, PLN);
        assertEquals(new BigDecimal("12.99"), money.amount());
        assertEquals(PLN, money.currency());
    }

    @Test
    void ofMinorUnitsHandlesZeroFractionCurrencies() {
        // JPY has 0 fraction digits: 1299 minor units == 1299 yen, not 12.99.
        assertEquals(new BigDecimal("1299"), Money.ofMinorUnits(1299, JPY).amount());
    }

    @Test
    void ofMinorUnitsByCodeMatchesByCurrency() {
        assertEquals(Money.ofMinorUnits(500, PLN), Money.ofMinorUnits(500, "PLN"));
    }

    @Test
    void ofMinorUnitsIsNegativeSafe() {
        assertEquals(new BigDecimal("-0.05"), Money.ofMinorUnits(-5, PLN).amount());
    }
}
