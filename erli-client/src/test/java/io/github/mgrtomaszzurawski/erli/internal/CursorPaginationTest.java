package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CursorPaginationTest {

    @Test
    void streamsAcrossPagesUntilNextCursorIsNull() {
        AtomicInteger fetches = new AtomicInteger();
        PageFetcher<String> fetcher = after -> {
            fetches.incrementAndGet();
            if (after == null) {
                return new Page<>(List.of("a", "b"), Cursor.of("c1"));
            }
            return new Page<>(List.of("c"), null);
        };

        List<String> all = CursorPagination.stream(fetcher).toList();

        assertEquals(List.of("a", "b", "c"), all);
        assertEquals(2, fetches.get());
    }

    @Test
    void emptyFirstPageYieldsEmptyStreamWithoutASecondFetch() {
        AtomicInteger fetches = new AtomicInteger();
        PageFetcher<String> fetcher = after -> {
            fetches.incrementAndGet();
            return new Page<>(List.of(), null);
        };

        List<String> all = CursorPagination.stream(fetcher).toList();

        assertEquals(List.of(), all);
        assertEquals(1, fetches.get());
    }

    @Test
    void stopsOnAnEmptyPageEvenWhenServerStillOffersACursor() {
        AtomicInteger fetches = new AtomicInteger();
        PageFetcher<String> fetcher = after -> {
            fetches.incrementAndGet();
            if (after == null) {
                return new Page<>(List.of("x"), Cursor.of("c1"));
            }
            // A stale/repeating cursor with no items must not loop forever.
            return new Page<>(List.of(), Cursor.of("c2"));
        };

        List<String> all = CursorPagination.stream(fetcher).toList();

        assertEquals(List.of("x"), all);
        assertEquals(2, fetches.get());
    }

    @Test
    void isLazyAndDoesNotFetchBeyondWhatIsConsumed() {
        AtomicInteger fetches = new AtomicInteger();
        PageFetcher<String> fetcher = after -> {
            fetches.incrementAndGet();
            Cursor next = after == null ? Cursor.of("c1") : Cursor.of("c-more");
            return new Page<>(List.of("item"), next);
        };

        try (Stream<String> stream = CursorPagination.stream(fetcher)) {
            List<String> firstOnly = stream.limit(1).toList();
            assertEquals(List.of("item"), firstOnly);
        }
        assertEquals(1, fetches.get(), "only the first page should have been fetched");
    }
}
