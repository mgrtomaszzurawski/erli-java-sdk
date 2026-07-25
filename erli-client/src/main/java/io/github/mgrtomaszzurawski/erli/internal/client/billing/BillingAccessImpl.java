package io.github.mgrtomaszzurawski.erli.internal.client.billing;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingAccess;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntry;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntryFilter;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.CursorPagination;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.Page;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesResponseInner;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * {@link BillingAccess} implementation over the shared {@link HttpRuntime}. Internal.
 *
 * <p>Both operations take the same request and return the same entry shape, so they differ only by
 * path. Neither echoes a pagination envelope — the response is a bare JSON array — so the next
 * cursor is <strong>derived</strong> from the last entry's id, which is exactly what the API's
 * {@code pagination.after} (with the only supported sort, {@code id} descending) consumes.
 */
public final class BillingAccessImpl implements BillingAccess {

    private final HttpRuntime runtime;

    public BillingAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public Stream<BillingEntry> entries(BillingEntryFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return CursorPagination.stream(after -> fetchEntriesPage(filter, after));
    }

    @Override
    public Stream<BillingEntry> rebates(BillingEntryFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return CursorPagination.stream(after -> fetchRebatesPage(filter, after));
    }

    private Page<BillingEntry> fetchEntriesPage(BillingEntryFilter filter, Cursor after) {
        List<BillingEntriesResponseInner> rawEntries = runtime.postList(ApiPaths.BILLING_COMPANY_ENTRIES,
                BillingMapper.toRaw(filter, after), BillingEntriesResponseInner.class);
        return toPage(rawEntries, filter.pageSize());
    }

    private Page<BillingEntry> fetchRebatesPage(BillingEntryFilter filter, Cursor after) {
        List<BillingEntriesResponseInner> rawEntries = runtime.postList(ApiPaths.BILLING_COMPANY_REBATES,
                BillingMapper.toRaw(filter, after), BillingEntriesResponseInner.class);
        return toPage(rawEntries, filter.pageSize());
    }

    private static Page<BillingEntry> toPage(List<BillingEntriesResponseInner> rawEntries, int pageSize) {
        List<BillingEntry> entries = rawEntries.stream().map(BillingMapper::toDomain).toList();
        return new Page<>(entries, nextCursor(entries, pageSize));
    }

    /**
     * A short page means the ledger is exhausted; a full page means there may be more, starting after
     * the lowest id seen (the sort is id-descending). Returning {@code null} stops the stream.
     */
    private static Cursor nextCursor(List<BillingEntry> entries, int pageSize) {
        if (entries.size() < pageSize) {
            return null;
        }
        return Cursor.of(Long.toString(entries.get(entries.size() - 1).id()));
    }
}
