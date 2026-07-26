package io.github.mgrtomaszzurawski.erli.internal.client.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Direct coverage of the money conversion shared by all four Finance areas (TESTING.md principle 2:
 * test {@code internal.*} directly rather than only through a facade). Every Erli finance amount is
 * a whole number of grosze, so a rounding or overflow bug here would silently corrupt settlement
 * figures in every area.
 */
class MinorUnitsTest {

    private static final String UNIT_PRICE_FIELD = "unitPrice";

    @ParameterizedTest(name = "{0} grosze -> {1} PLN")
    @CsvSource({
            "1168, 11.68",
            "0, 0.00",
            "1, 0.01",
            "100, 1.00",
            "-1168, -11.68",
            "2147483647, 21474836.47",
    })
    void convertsGroszeToMoney(long grosze, String expectedPln) {
        Money money = MinorUnits.fromGrosze(grosze);

        assertEquals(new BigDecimal(expectedPln), money.amount());
        assertEquals(MinorUnits.PLN, money.currency());
    }

    @Test
    void keepsTwoDecimalScaleSoAmountsFormatAsCurrency() {
        assertEquals("11.68", MinorUnits.fromGrosze(1168).amount().toPlainString());
        assertEquals("5.00", MinorUnits.fromGrosze(500).amount().toPlainString());
    }

    @ParameterizedTest(name = "{0} PLN -> {1} grosze")
    @CsvSource({
            "100.00, 10000",
            "0.01, 1",
            "0, 0",
            "11.68, 1168",
            "-11.68, -1168",
    })
    void convertsMoneyToGrosze(String pln, int expectedGrosze) {
        assertEquals(expectedGrosze, MinorUnits.toGrosze(Money.ofPln(pln), UNIT_PRICE_FIELD));
    }

    @Test
    void roundTripsThroughGroszeWithoutDrift() {
        Money original = Money.ofPln("1234.56");

        assertEquals(original.amount(), MinorUnits.fromGrosze(
                MinorUnits.toGrosze(original, UNIT_PRICE_FIELD)).amount());
    }

    @Test
    void rejectsACurrencyOtherThanZloty() {
        Money euroAmount = Money.of("100.00", "EUR");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> MinorUnits.toGrosze(euroAmount, UNIT_PRICE_FIELD));

        assertTrue(failure.getMessage().contains(UNIT_PRICE_FIELD), failure.getMessage());
        assertTrue(failure.getMessage().contains("PLN"), failure.getMessage());
        assertTrue(failure.getMessage().contains("EUR"), failure.getMessage());
    }

    @Test
    void rejectsAFractionOfAGroszRatherThanRoundingItAway() {
        // Silent rounding here would under- or over-charge; the caller must decide.
        Money fractionalGroszAmount = Money.ofPln("10.005");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> MinorUnits.toGrosze(fractionalGroszAmount, UNIT_PRICE_FIELD));

        assertTrue(failure.getMessage().contains("whole number of grosze"), failure.getMessage());
    }

    @Test
    void rejectsAnAmountTooLargeForTheWire() {
        // The API caps money fields at a signed 32-bit grosz value.
        Money aboveWireCapAmount = Money.ofPln("21474836.48");
        assertThrows(IllegalArgumentException.class,
                () -> MinorUnits.toGrosze(aboveWireCapAmount, UNIT_PRICE_FIELD));
    }

    @Test
    void rejectsANullAmountWithTheFieldNameInTheMessage() {
        NullPointerException failure = assertThrows(NullPointerException.class,
                () -> MinorUnits.toGrosze(null, UNIT_PRICE_FIELD));

        assertTrue(failure.getMessage().contains(UNIT_PRICE_FIELD), failure.getMessage());
    }

    @Test
    void wrapsAnAmountAlreadyExpressedInZloty() {
        // Payment.amount is the one Erli money field sent in złoty rather than grosze.
        Money money = MinorUnits.fromMajorUnits(new BigDecimal("49.99"));

        assertEquals(new BigDecimal("49.99"), money.amount());
        assertEquals(MinorUnits.PLN, money.currency());
    }
}
