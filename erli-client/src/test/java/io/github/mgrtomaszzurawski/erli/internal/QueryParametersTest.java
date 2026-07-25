package io.github.mgrtomaszzurawski.erli.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class QueryParametersTest {

    private static final String BASE = "https://host/svc";

    @Test
    void emptyLeavesTheBaseUnchanged() {
        assertEquals(BASE, QueryParameters.empty().appendTo(BASE));
    }

    @Test
    void skipsNullValuesAndKeepsInsertionOrder() {
        String uri = QueryParameters.builder()
                .add("id", "42")
                .add("missing", null)
                .addBoolean("cod", Boolean.TRUE)
                .addBoolean("absent", null)
                .build()
                .appendTo(BASE);
        assertEquals(BASE + "?id=42&cod=true", uri);
    }

    @Test
    void commaJoinsCsvValuesAndKeepsTheCommaLiteral() {
        String uri = QueryParameters.builder()
                .addCsv("parcelIds", List.of("1", "2", "3"))
                .build()
                .appendTo(BASE);
        assertEquals(BASE + "?parcelIds=1,2,3", uri);
    }

    @Test
    void repeatedAndBracketRepeatedProduceMultiplePairs() {
        assertEquals(BASE + "?id=1&id=2",
                QueryParameters.builder().addRepeated("id", List.of("1", "2")).build().appendTo(BASE));
        assertEquals(BASE + "?name%5B%5D=a&name%5B%5D=b",
                QueryParameters.builder().addBracketRepeated("name", List.of("a", "b")).build().appendTo(BASE));
    }

    @Test
    void urlEncodesReservedCharactersInValues() {
        String uri = QueryParameters.builder()
                .add("q", "a&b=c")
                .build()
                .appendTo(BASE);
        assertEquals(BASE + "?q=a%26b%3Dc", uri);
    }
}
