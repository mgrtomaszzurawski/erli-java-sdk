package io.github.mgrtomaszzurawski.erli.internal;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void rejectsDotAndDotDotSegments() {
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", Map.of("id", "..")));
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", Map.of("id", ".")));
    }

    @Test
    void rejectsBlankValue() {
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", Map.of("id", "  ")));
    }

    @Test
    void rejectsUnresolvedPlaceholder() {
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", Map.of()));
    }

    @Test
    void rejectsUnknownPlaceholderName() {
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand("/orders/{id}", Map.of("wrong", "1")));
    }
}
