package io.github.mgrtomaszzurawski.erli.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PathTemplateTest {

    @Test
    void expandsAndEncodesTheSegment() {
        assertEquals("/products/sku-1",
                PathTemplate.expand("/products/{externalId}", Map.of("externalId", "sku-1")));
    }

    @Test
    void encodesReservedCharactersThatWouldRerouteTheRequest() {
        // '#','/','?' in an id must not split the path or add a fragment/query.
        assertEquals("/orders/a%23b%2Fc%3Fd",
                PathTemplate.expand("/orders/{id}", Map.of("id", "a#b/c?d")));
    }

    @Test
    void encodesSpaceAsPercent20NotPlus() {
        assertEquals("/hooks/on%20order",
                PathTemplate.expand("/hooks/{hookName}", Map.of("hookName", "on order")));
    }

    /**
     * Carried over from the Orders bucket's own path-encoding tests. Seller-supplied identifiers are
     * routinely Polish, so the UTF-8 byte encoding is a real case rather than a theoretical one.
     */
    @Test
    void encodesNonAsciiAsUtf8Bytes() {
        assertEquals("/orders/%C5%82%C3%B3d%C5%BA",
                PathTemplate.expand("/orders/{id}", Map.of("id", "łódź")));
    }

    /**
     * A literal {@code +} must survive as {@code %2B} and not be confused with the encoded-space fix.
     */
    @Test
    void encodesALiteralPlusRatherThanTreatingItAsASpace() {
        assertEquals("/orders/a%2Bb", PathTemplate.expand("/orders/{id}", Map.of("id", "a+b")));
    }

    @Test
    void rejectsDotAndDotDotSegments() {
        Map<String, String> dotDotSegment = Map.of("id", "..");
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", dotDotSegment));
        Map<String, String> dotSegment = Map.of("id", ".");
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", dotSegment));
    }

    @Test
    void rejectsBlankValue() {
        Map<String, String> blankValue = Map.of("id", "  ");
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", blankValue));
    }

    @Test
    void rejectsUnresolvedPlaceholder() {
        Map<String, String> noParameters = Map.of();
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", noParameters));
    }

    @Test
    void rejectsUnknownPlaceholderName() {
        Map<String, String> unknownPlaceholder = Map.of("wrong", "1");
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", unknownPlaceholder));
    }
}
