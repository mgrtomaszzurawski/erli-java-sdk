package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.error.ErliApiException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Payments in, payouts out, and the operator's transaction history. Reached via
 * {@code client.payments()}.
 *
 * <p>The API exposes all three behind one pair of endpoints selected by a {@code type} discriminator;
 * this facade splits them into typed methods so a result never has to be downcast. Searches return a
 * lazy {@link Stream} — pages are fetched only as the stream is consumed.
 */
public interface PaymentsAccess {

    /**
     * Walk the shop's buyer payments ({@code POST /payments/operations/_search}).
     *
     * @param search which payments to return; use {@link PaymentSearch#all()} for everything
     * @return a lazy stream over every matching payment
     * @throws ErliApiException       if the API reports an error
     * @throws ErliTransportException if a request could not be completed or decoded
     */
    Stream<Payment> searchPayments(PaymentSearch search);

    /**
     * Walk the shop's payouts ({@code POST /payments/operations/_search}).
     *
     * @param search which payouts to return; use {@link PayoutSearch#all()} for everything
     * @return a lazy stream over every matching payout
     * @throws ErliApiException       if the API reports an error
     * @throws ErliTransportException if a request could not be completed or decoded
     */
    Stream<Payout> searchPayouts(PayoutSearch search);

    /**
     * Walk the operator's transaction history ({@code POST /payments/operations/_search}).
     *
     * <p>This one is page-numbered rather than cursor-based, and needs a bounded event-date range.
     * It is backed by the payment operator, so it can fail with a not-found error when the shop has
     * no operator account yet.
     *
     * @param search the event-date range and any narrowing criteria
     * @return a lazy stream over every matching transaction
     * @throws ErliApiException       if the API reports an error
     * @throws ErliTransportException if a request could not be completed or decoded
     */
    Stream<Transaction> searchReturns(ReturnSearch search);

    /**
     * Fetch one payment by id ({@code GET /payments/operations/{id}}).
     *
     * @param paymentId the payment's identifier
     * @return the payment, or empty if the shop has no payment with that id
     * @throws ErliApiException       if the API reports an error other than not-found
     * @throws ErliTransportException if the request could not be completed or decoded
     */
    Optional<Payment> findPayment(long paymentId);

    /**
     * Fetch one payout by id ({@code GET /payments/operations/{id}}).
     *
     * @param payoutId the payout's identifier
     * @return the payout, or empty if the company has no payout with that id
     * @throws ErliApiException       if the API reports an error other than not-found
     * @throws ErliTransportException if the request could not be completed or decoded
     */
    Optional<Payout> findPayout(long payoutId);
}
