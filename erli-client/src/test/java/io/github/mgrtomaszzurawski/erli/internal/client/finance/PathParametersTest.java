package io.github.mgrtomaszzurawski.erli.internal.client.finance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Path templating is one of only two places caller data becomes part of a request line, so its
 * encoding is a correctness <em>and</em> a safety property.
 */
class PathParametersTest {

    private static final String PATH_TEMPLATE = "/payments/operations/{id}";
    private static final String ID_PLACEHOLDER = "id";

    @Test
    void substitutesThePlaceholder() {
        assertEquals("/payments/operations/42", PathParameters.fill(PATH_TEMPLATE, ID_PLACEHOLDER, "42"));
    }

    @Test
    void encodesAValueSoItCannotEscapeItsSegment() {
        String path = PathParameters.fill(PATH_TEMPLATE, ID_PLACEHOLDER, "../../me?x=1");

        assertFalse(path.contains("../"), path);
        assertFalse(path.contains("?"), path);
        assertEquals("/payments/operations/..%2F..%2Fme%3Fx%3D1", path);
    }

    @Test
    void encodesASpaceAsPercentTwentyNotPlus() {
        // "+" means a space only in a query; in a path it is a literal plus.
        assertEquals("/payments/operations/a%20b", PathParameters.fill(PATH_TEMPLATE, ID_PLACEHOLDER, "a b"));
    }

    @Test
    void leavesAnUnrelatedPlaceholderAlone() {
        assertEquals("/a/1/{other}", PathParameters.fill("/a/{id}/{other}", ID_PLACEHOLDER, "1"));
    }
}
