package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;

import java.util.List;

/**
 * One page returned by an Erli {@code POST .../_search} endpoint: the items plus the
 * {@code pagination.after} cursor for the next page (or {@code null} on the last page). Internal.
 *
 * @param items      the items on this page (defensively copied)
 * @param nextCursor the cursor to fetch the next page, or {@code null} if this is the last page
 * @param <T>        the item type
 */
public record Page<T>(List<T> items, Cursor nextCursor) {

    public Page {
        items = List.copyOf(items);
    }
}
