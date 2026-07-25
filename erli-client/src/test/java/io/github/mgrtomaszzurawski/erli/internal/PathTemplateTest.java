package io.github.mgrtomaszzurawski.erli.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Path-segment encoding contract. Identifiers reach the SDK from consumer data, so a value must never
 * be able to change the shape of the request path.
 */
class PathTemplateTest {

    private static final String STATUS_TEMPLATE = "/orders/{id}/status";
    private static final String ID_PLACEHOLDER = "{id}";

    @Test
    void leavesAnOrdinaryIdentifierUntouched() {
        assertEquals("/orders/221201x12345/status",
                PathTemplate.expand(STATUS_TEMPLATE, ID_PLACEHOLDER, "221201x12345"));
    }

    @Test
    void leavesUnreservedPunctuationUntouched() {
        assertEquals("/orders/a-b_c.d~e/status",
                PathTemplate.expand(STATUS_TEMPLATE, ID_PLACEHOLDER, "a-b_c.d~e"));
    }

    /**
     * The reason this class exists. Before encoding, an id ending in {@code #} truncated the template
     * so that {@code PATCH /orders/{id}/status} was sent as {@code PATCH /orders/{id}} — a different,
     * successful, authenticated write against the wrong resource.
     */
    @ParameterizedTest(name = "{0} cannot escape its segment")
    @CsvSource({
            "'221201x1#',            /orders/221201x1%23/status",
            "'221201x1?limit=999',   /orders/221201x1%3Flimit%3D999/status",
            "'221201x1/status',      /orders/221201x1%2Fstatus/status",
            "'../../shops/me',       /orders/..%2F..%2Fshops%2Fme/status",
            "'a b',                  /orders/a%20b/status",
    })
    void encodesCharactersThatWouldOtherwiseAlterThePath(String hostileId, String expectedPath) {
        assertEquals(expectedPath, PathTemplate.expand(STATUS_TEMPLATE, ID_PLACEHOLDER, hostileId));
    }

    @Test
    void encodesNonAsciiAsUtf8() {
        assertEquals("/orders/%C5%82/status", PathTemplate.expand(STATUS_TEMPLATE, ID_PLACEHOLDER, "ł"));
    }

    /**
     * Percent-encoding cannot neutralize these: both are made only of unreserved characters, so they
     * would survive encoding and still resolve as relative path segments.
     */
    @ParameterizedTest
    @ValueSource(strings = {".", ".."})
    void rejectsRelativePathSegments(String relativeSegment) {
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand(STATUS_TEMPLATE, ID_PLACEHOLDER, relativeSegment));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void rejectsABlankValue(String blank) {
        assertThrows(IllegalArgumentException.class,
                () -> PathTemplate.expand(STATUS_TEMPLATE, ID_PLACEHOLDER, blank));
    }
}
