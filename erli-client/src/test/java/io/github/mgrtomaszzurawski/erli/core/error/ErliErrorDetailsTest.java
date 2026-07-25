package io.github.mgrtomaszzurawski.erli.core.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErliErrorDetailsTest {

    @Test
    void toStringRedactsRawBodyAndPayloadButKeepsDiagnostics() {
        ErliErrorDetails details = new ErliErrorDetails(
                400, 400, "validation", "BadRequest", "Invalid buyer e-mail",
                "Nieprawidłowy e-mail", "trace-1", "span-1",
                "buyer@example.com", "{\"email\":\"buyer@example.com\",\"address\":\"1 Main St\"}");

        String rendered = details.toString();

        // PII carriers must not appear in the log-safe rendering.
        assertFalse(rendered.contains("buyer@example.com"), "payload/rawBody PII leaked into toString");
        assertFalse(rendered.contains("1 Main St"), "rawBody address leaked into toString");
        // Diagnostics that help debugging are kept.
        assertTrue(rendered.contains("400"));
        assertTrue(rendered.contains("validation"));
        assertTrue(rendered.contains("trace-1"));
    }
}
