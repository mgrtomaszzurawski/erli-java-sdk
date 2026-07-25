package io.github.mgrtomaszzurawski.erli.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiKeyTest {

    private static final String RAW_KEY = "id-part:secret-part";

    @Test
    void producesBearerAuthorizationHeader() {
        ApiKey key = ApiKey.of(RAW_KEY);
        assertEquals("Bearer " + RAW_KEY, key.authorizationHeaderValue());
    }

    @Test
    void neverExposesTokenInRedactionOrToString() {
        ApiKey key = ApiKey.of(RAW_KEY);
        assertFalse(key.redacted().contains(RAW_KEY), "redacted() must not contain the token");
        assertFalse(key.toString().contains(RAW_KEY), "toString() must not contain the token");
        assertFalse(key.toString().contains("secret-part"), "toString() must not leak any token part");
    }

    @Test
    void trimsInputAndRejectsBlank() {
        assertEquals("Bearer " + RAW_KEY, ApiKey.of("  " + RAW_KEY + "  ").authorizationHeaderValue());
        assertThrows(IllegalArgumentException.class, () -> ApiKey.of("   "));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> ApiKey.of(null));
    }

    @Test
    void isValueEqualOnToken() {
        assertEquals(ApiKey.of(RAW_KEY), ApiKey.of(RAW_KEY));
        assertNotEquals(ApiKey.of(RAW_KEY), ApiKey.of("other:key"));
    }
}
