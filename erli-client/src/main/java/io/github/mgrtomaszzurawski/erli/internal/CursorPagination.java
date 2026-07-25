package io.github.mgrtomaszzurawski.erli.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Turns a {@link PageFetcher} into a lazy {@link Stream} over the body cursor. Pages are fetched on
 * demand as the stream is consumed — the first fetch happens on the first terminal pull, and the next
 * page is only fetched when the current one is drained. Iteration stops when a page reports no next
 * cursor or comes back empty (both signal the end of an Erli {@code _search}). Internal.
 */
public final class CursorPagination {

    private CursorPagination() {
    }

    /** A sequential, ordered, lazy stream over all items across all pages. */
    public static <T> Stream<T> stream(PageFetcher<T> fetcher) {
        return StreamSupport.stream(new CursorSpliterator<>(fetcher), false);
    }

    private static final class CursorSpliterator<T> implements Spliterator<T> {

        private final PageFetcher<T> fetcher;
        private Iterator<T> current = Collections.emptyIterator();
        private io.github.mgrtomaszzurawski.erli.core.model.Cursor nextCursor;
        private boolean firstFetchDone;
        private boolean noMorePages;

        private CursorSpliterator(PageFetcher<T> fetcher) {
            this.fetcher = fetcher;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            while (!current.hasNext()) {
                if (noMorePages) {
                    return false;
                }
                Page<T> page = fetcher.fetch(firstFetchDone ? nextCursor : null);
                firstFetchDone = true;
                current = page.items().iterator();
                nextCursor = page.nextCursor();
                // Terminal when the server offers no further cursor or returns an empty page; the
                // empty-page guard also prevents an infinite loop on a stale/repeating cursor.
                if (nextCursor == null || page.items().isEmpty()) {
                    noMorePages = true;
                }
            }
            action.accept(current.next());
            return true;
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED | NONNULL;
        }
    }
}
