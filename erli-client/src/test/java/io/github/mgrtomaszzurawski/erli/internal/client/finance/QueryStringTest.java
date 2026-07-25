package io.github.mgrtomaszzurawski.erli.internal.client.finance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The query/path builder is the SDK's only place where caller data becomes part of a request line,
 * so its encoding is a correctness <em>and</em> a safety property.
 */
class QueryStringTest {

    private static final String PATH_TEMPLATE = "/payments/operations/{id}";
    private static final String ID_PLACEHOLDER = "id";

    @Test
    void rendersNothingWhenNoParameterWasSet() {
        assertEquals("", new QueryString().render());
    }

    @Test
    void rendersParametersInInsertionOrder() {
        String rendered = new QueryString()
                .add("startDate", "2026-06-01")
                .add("endDate", "2026-07-20")
                .render();

        assertEquals("?startDate=2026-06-01&endDate=2026-07-20", rendered);
    }

    @Test
    void skipsNullValuesBecauseAbsentIsNotTheSameAsEmpty() {
        String rendered = new QueryString().add("type", "payment").add("shopId", null).render();

        assertEquals("?type=payment", rendered);
        assertFalse(rendered.contains("shopId"));
    }

    @Test
    void encodesValuesSoTheyCannotInjectASeparator() {
        String rendered = new QueryString().add("type", "payment&admin=true").render();

        assertEquals("?type=payment%26admin%3Dtrue", rendered);
    }

    @Test
    void substitutesAPathParameter() {
        assertEquals("/payments/operations/42",
                QueryString.pathParameter(PATH_TEMPLATE, ID_PLACEHOLDER, "42"));
    }

    @Test
    void encodesAPathParameterSoItCannotEscapeItsSegment() {
        String path = QueryString.pathParameter(PATH_TEMPLATE, ID_PLACEHOLDER, "../../me?x=1");

        assertFalse(path.contains("../"), path);
        assertFalse(path.contains("?"), path);
        assertEquals("/payments/operations/..%2F..%2Fme%3Fx%3D1", path);
    }

    @Test
    void encodesASpaceInAPathSegmentAsPercentTwentyNotPlus() {
        // "+" means a space only in a query; in a path it is a literal plus.
        assertEquals("/payments/operations/a%20b",
                QueryString.pathParameter(PATH_TEMPLATE, ID_PLACEHOLDER, "a b"));
    }
}
