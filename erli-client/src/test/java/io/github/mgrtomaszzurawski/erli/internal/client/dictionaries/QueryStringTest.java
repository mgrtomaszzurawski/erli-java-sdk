package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryStringTest {

    @Test
    void producesNothingWhenNoParameterIsPresent() {
        String query = QueryString.builder()
                .add("id", Optional.empty())
                .add("cod", Optional.empty())
                .build();

        assertEquals("", query);
    }

    @Test
    void dropsAbsentParametersAndKeepsPresentOnes() {
        String query = QueryString.builder()
                .add("id", Optional.empty())
                .add("cod", Optional.of(true))
                .add("vendor", Optional.empty())
                .build();

        assertEquals("?cod=true", query);
    }

    @Test
    void joinsSeveralParametersInTheOrderTheyWereAdded() {
        String query = QueryString.builder()
                .add("id", Optional.of("erliPaczkomat"))
                .add("cod", Optional.of(false))
                .build();

        assertEquals("?id=erliPaczkomat&cod=false", query);
    }

    @Test
    void percentEncodesValuesSoAFilterCannotAlterTheRequest() {
        String query = QueryString.builder()
                .add("name", Optional.of("Dom & Ogród"))
                .build();

        assertEquals("?name=Dom+%26+Ogr%C3%B3d", query);
    }

    @Test
    void percentEncodesAValueThatWouldOtherwiseInjectAnotherParameter() {
        String query = QueryString.builder()
                .add("name", Optional.of("x&admin=true"))
                .build();

        assertEquals("?name=x%26admin%3Dtrue", query);
    }
}
