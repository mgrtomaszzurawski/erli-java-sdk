package io.github.mgrtomaszzurawski.erli.domain.billing;

import io.github.mgrtomaszzurawski.erli.core.error.ErliApiException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

import java.util.stream.Stream;

/**
 * The company's settlement ledger with Erli. Reached via {@code client.billing()}.
 *
 * <p>Both operations cover the <strong>whole company</strong>, i.e. every shop it owns; narrow to one
 * shop with {@link BillingEntryFilter.Builder#shopId(long)}. Results are returned newest-first as a
 * lazy {@link Stream} — pages are fetched only as the stream is consumed, so
 * {@code entries(filter).limit(20)} performs a single request.
 */
public interface BillingAccess {

    /**
     * Walk the company's settlement history ({@code POST /billing/company/entries}).
     *
     * @param filter which entries to return; use {@link BillingEntryFilter#all()} for everything
     * @return a lazy, newest-first stream over every matching entry
     * @throws ErliApiException       if the API reports an error
     * @throws ErliTransportException if a request could not be completed or decoded
     */
    Stream<BillingEntry> entries(BillingEntryFilter filter);

    /**
     * Walk the company's rebate reserve and history ({@code POST /billing/company/rebates}).
     *
     * <p>Same entry shape as {@link #entries}, restricted to rebates; the
     * {@link BillingEntry#rebateOrigin()} breakdown is the interesting part here.
     *
     * @param filter which entries to return; use {@link BillingEntryFilter#all()} for everything
     * @return a lazy, newest-first stream over every matching rebate entry
     * @throws ErliApiException       if the API reports an error
     * @throws ErliTransportException if a request could not be completed or decoded
     */
    Stream<BillingEntry> rebates(BillingEntryFilter filter);
}
