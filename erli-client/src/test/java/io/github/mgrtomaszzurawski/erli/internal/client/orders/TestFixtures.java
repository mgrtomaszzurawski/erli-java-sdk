package io.github.mgrtomaszzurawski.erli.internal.client.orders;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Loads a JSON fixture from the test classpath. */
final class TestFixtures {

    private TestFixtures() {
    }

    static String read(String resourcePath) {
        try (InputStream stream = TestFixtures.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Fixture not found on the test classpath: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to read fixture " + resourcePath, failure);
        }
    }
}
