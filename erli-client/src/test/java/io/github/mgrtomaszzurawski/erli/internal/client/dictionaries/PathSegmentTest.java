package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathSegmentTest {

    @Test
    void leavesAPlainSegmentUnchanged() {
        assertEquals("standardowy", PathSegment.encode("standardowy"));
    }

    @Test
    void encodesASpaceForAPathSegmentRatherThanAForm() {
        // URLEncoder would give "+", which is a literal plus inside a path segment.
        assertEquals("cennik%20letni", PathSegment.encode("cennik letni"));
    }

    @Test
    void encodesASlashSoTheSegmentCannotChangeTheEndpoint() {
        assertEquals("a%2F..%2Fme", PathSegment.encode("a/../me"));
    }

    @Test
    void encodesNonAsciiAsUtf8() {
        assertEquals("cennik-%C5%82%C3%B3d%C5%BA", PathSegment.encode("cennik-łódź"));
    }
}
