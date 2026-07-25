package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;

/**
 * Fetches a single page given the cursor to start after. Buckets implement this by POSTing to their
 * {@code _search} endpoint with {@code pagination.after} set to the given cursor; the SDK drives the
 * iteration lazily via {@link CursorPagination}. Internal.
 *
 * @param <T> the item type
 */
@FunctionalInterface
public interface PageFetcher<T> {

    /**
     * Fetch the page that follows the given cursor.
     *
     * @param after the cursor to start after, or {@code null} for the first page
     * @return the next page of items and the following cursor
     */
    Page<T> fetch(Cursor after);
}
