package io.github.mgrtomaszzurawski.erli.internal.client.payments;

import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.domain.payments.Payment;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentsAccess;
import io.github.mgrtomaszzurawski.erli.domain.payments.Payout;
import io.github.mgrtomaszzurawski.erli.domain.payments.PayoutSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.ReturnSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.Transaction;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.CursorPagination;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.Page;
import io.github.mgrtomaszzurawski.erli.internal.client.finance.MinorUnits;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;
import io.github.mgrtomaszzurawski.erli.internal.client.finance.PathParameters;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * {@link PaymentsAccess} implementation over the shared {@link HttpRuntime}. Internal.
 *
 * <p>All three searches share one endpoint, discriminated by the {@code type} field in the request
 * body (not the query parameter the spec describes — see {@code KNOWN-SERVER-BEHAVIORS.md}). None of
 * them echoes a pagination envelope: the response is a bare JSON array, so payments and payouts
 * derive their next cursor from the last item's sort field, and returns page by number instead.
 */
public final class PaymentsAccessImpl implements PaymentsAccess {

    private static final String ID_PLACEHOLDER = "id";
    private static final String TYPE_PARAMETER = "type";
    private static final String AMOUNT_FIELD = "amount";
    private static final int FIRST_PAGE = 1;

    private final HttpRuntime runtime;

    public PaymentsAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public Stream<Payment> searchPayments(PaymentSearch search) {
        Objects.requireNonNull(search, "search");
        return CursorPagination.stream(after -> fetchPaymentsPage(search, after));
    }

    @Override
    public Stream<Payout> searchPayouts(PayoutSearch search) {
        Objects.requireNonNull(search, "search");
        return CursorPagination.stream(after -> fetchPayoutsPage(search, after));
    }

    @Override
    public Stream<Transaction> searchReturns(ReturnSearch search) {
        Objects.requireNonNull(search, "search");
        // This endpoint is page-numbered, so the cursor spliterator is driven by a page counter
        // rather than by a value taken from the last item.
        AtomicInteger pageNumber = new AtomicInteger(FIRST_PAGE);
        return CursorPagination.stream(after -> fetchReturnsPage(search, pageNumber.getAndIncrement()));
    }

    @Override
    public Optional<Payment> findPayment(long paymentId) {
        try {
            var rawPayment = runtime.get(operationPath(ApiPaths.PAYMENT_OPERATION_BY_ID, paymentId), typeQuery(PaymentSearchMapper.TYPE_PAYMENT),
                    io.github.mgrtomaszzurawski.erli.rest.model.Payment.class);
            return Optional.of(PaymentMapper.toPayment(rawPayment));
        } catch (ErliNotFoundException absent) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Payout> findPayout(long payoutId) {
        try {
            var rawPayout = runtime.get(operationPath(ApiPaths.PAYMENT_OPERATION_BY_ID, payoutId), typeQuery(PaymentSearchMapper.TYPE_PAYOUT),
                    io.github.mgrtomaszzurawski.erli.rest.model.Payout.class);
            return Optional.of(PaymentMapper.toPayout(rawPayout));
        } catch (ErliNotFoundException absent) {
            return Optional.empty();
        }
    }

    private static String operationPath(String pathTemplate, long operationId) {
        return PathParameters.fill(pathTemplate, ID_PLACEHOLDER, Long.toString(operationId));
    }

    /**
     * Unlike the search — where the server reads the discriminator from the body — this operation
     * really does take {@code type} as a query parameter.
     */
    private static QueryParameters typeQuery(String type) {
        return QueryParameters.builder().add(TYPE_PARAMETER, type).build();
    }

    private Page<Payment> fetchPaymentsPage(PaymentSearch search, Cursor after) {
        List<io.github.mgrtomaszzurawski.erli.rest.model.Payment> rawPayments = runtime.postList(ApiPaths.PAYMENT_OPERATIONS_SEARCH,
                PaymentSearchMapper.toPaymentBody(search, after),
                io.github.mgrtomaszzurawski.erli.rest.model.Payment.class);
        List<Payment> payments = rawPayments.stream().map(PaymentMapper::toPayment).toList();
        return new Page<>(payments, nextPaymentCursor(payments, search));
    }

    private Page<Payout> fetchPayoutsPage(PayoutSearch search, Cursor after) {
        List<io.github.mgrtomaszzurawski.erli.rest.model.Payout> rawPayouts = runtime.postList(ApiPaths.PAYMENT_OPERATIONS_SEARCH,
                PaymentSearchMapper.toPayoutBody(search, after),
                io.github.mgrtomaszzurawski.erli.rest.model.Payout.class);
        List<Payout> payouts = rawPayouts.stream().map(PaymentMapper::toPayout).toList();
        return new Page<>(payouts, nextPayoutCursor(payouts, search));
    }

    private Page<Transaction> fetchReturnsPage(ReturnSearch search, int pageNumber) {
        List<io.github.mgrtomaszzurawski.erli.rest.model.Transaction> rawTransactions = runtime.postList(ApiPaths.PAYMENT_OPERATIONS_SEARCH,
                PaymentSearchMapper.toReturnBody(search, pageNumber),
                io.github.mgrtomaszzurawski.erli.rest.model.Transaction.class);
        List<Transaction> transactions =
                rawTransactions.stream().map(PaymentMapper::toTransaction).toList();
        // A short page is the last one; otherwise hand back any non-null cursor so the spliterator
        // asks again and the page counter advances.
        Cursor next = transactions.size() < search.pageSize() ? null : Cursor.of(Integer.toString(pageNumber));
        return new Page<>(transactions, next);
    }

    /**
     * The cursor is compared against the sort field, so it must be that field's value from the last
     * item on the page. A short page means there is nothing left to ask for.
     */
    private static Cursor nextPaymentCursor(List<Payment> payments, PaymentSearch search) {
        if (payments.size() < search.pageSize()) {
            return null;
        }
        Payment last = payments.get(payments.size() - 1);
        String cursorValue = switch (search.sortField()) {
            case ID -> Long.toString(last.id());
            case CREATED_AT -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(last.createdAt());
            // A payment still in flight has no completedAt; without one there is no cursor to
            // continue from, so stop rather than send a malformed one.
            case COMPLETED_AT -> last.completedAt()
                    .map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format)
                    .orElse(null);
        };
        return cursorValue == null ? null : Cursor.of(cursorValue);
    }

    private static Cursor nextPayoutCursor(List<Payout> payouts, PayoutSearch search) {
        if (payouts.size() < search.pageSize()) {
            return null;
        }
        Payout last = payouts.get(payouts.size() - 1);
        return Cursor.of(switch (search.sortField()) {
            case ID -> Long.toString(last.id());
            case CREATED_AT -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(last.createdAt());
            case AMOUNT -> Integer.toString(MinorUnits.toGrosze(last.amount(), AMOUNT_FIELD));
        });
    }
}
