package io.github.mgrtomaszzurawski.erli.internal.client.payments;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.model.Market;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.PaymentStatus;
import io.github.mgrtomaszzurawski.erli.core.model.SortOrder;
import io.github.mgrtomaszzurawski.erli.domain.payments.Payment;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentOperator;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSortField;
import io.github.mgrtomaszzurawski.erli.domain.payments.Payout;
import io.github.mgrtomaszzurawski.erli.domain.payments.PayoutSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.ReturnSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.Transaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify-on-write coverage for the payments area. The two behaviours worth guarding hardest are the
 * discriminator living in the request <em>body</em> (the spec says query) and the client-derived
 * cursor, since the response carries no pagination envelope.
 */
class PaymentsAccessImplTest {

    private static final String SEARCH_PATH = "/payments/operations/_search";
    private static final String OPERATION_PATH = "/payments/operations/1";
    private static final String API_KEY_VALUE = "100007:test-secret";

    /** Built from the Payment schema; the sandbox has no payments to copy from yet. */
    private static final String ONE_PAYMENT_BODY = """
            [{"id":77,"orderIds":[1234],"amount":149.99,"status":"COMPLETED",
              "createdAt":"2026-07-24T10:00:00.000+02:00","completedAt":"2026-07-24T10:05:00.000+02:00",
              "operator":"PAYU","methodCode":"PAYU.blik","methodName":"BLIK",
              "externalPaymentId":"PAYU-XYZ"}]""";

    private static final String TWO_PAYMENTS_BODY = """
            [{"id":77,"orderIds":[1234],"amount":149.99,"status":"COMPLETED",
              "createdAt":"2026-07-24T10:00:00.000+02:00","completedAt":"2026-07-24T10:05:00.000+02:00",
              "operator":"PAYU","methodCode":"PAYU.blik"},
             {"id":78,"orderIds":[1235],"amount":10.00,"status":"PENDING",
              "createdAt":"2026-07-24T11:00:00.000+02:00","completedAt":"2026-07-24T11:05:00.000+02:00",
              "operator":"PAYU","methodCode":"PAYU.c"}]""";

    private static final String ONE_PAYOUT_BODY = """
            [{"id":9,"amount":250000,"createdAt":"2026-07-20T08:00:00.000+02:00","operator":"PAYU"}]""";

    private static final String EMPTY_BODY = "[]";

    private WireMockServer server;
    private ErliClient client;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = ErliClient.builder()
                .baseUrl(server.baseUrl())
                .apiKey(ApiKey.of(API_KEY_VALUE))
                .build();
    }

    @AfterEach
    void stopServer() {
        client.close();
        server.stop();
    }

    @Test
    void sendsTheTypeDiscriminatorInTheBodyNotTheQuery() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(EMPTY_BODY)));

        client.payments().searchPayments(PaymentSearch.all()).toList();

        // The spec declares type as a required query parameter; the live server ignores it there and
        // reads it from the body (KNOWN-SERVER-BEHAVIORS.md). urlEqualTo pins the absent query.
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withHeader("Authorization", equalTo("Bearer " + API_KEY_VALUE))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.type", equalTo("payment")))
                .withRequestBody(equalToJson("""
                        {"type":"payment","pagination":{"sortField":"createdAt","order":"ASC","limit":50}}""")));
    }

    @Test
    void mapsEveryPaymentField() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(ONE_PAYMENT_BODY)));

        Payment payment = client.payments().searchPayments(PaymentSearch.all()).findFirst().orElseThrow();

        assertEquals(77L, payment.id());
        assertEquals(List.of(OrderId.of("1234")), payment.orderIds());
        // Payment.amount is in złoty, unlike every other Finance amount.
        assertEquals(Money.ofPln("149.99"), payment.amount());
        assertEquals(PaymentStatus.COMPLETED, payment.status());
        assertEquals("PAYU.blik", payment.methodCode().orElseThrow());
        assertEquals("BLIK", payment.methodName().orElseThrow());
        assertEquals("PAYU-XYZ", payment.externalPaymentId().orElseThrow());
        // Guards the other direction of the CORE-12 sentinel: without this, toOperator could return
        // UNRECOGNIZED for everything and the suite would stay green.
        assertEquals(PaymentOperator.PAYU, payment.operator());
        assertTrue(payment.completedAt().isPresent());
    }

    @Test
    void sendsPayoutSearchWithItsOwnTypeAndSort() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(ONE_PAYOUT_BODY)));

        Payout payout = client.payments().searchPayouts(PayoutSearch.all()).findFirst().orElseThrow();

        // Payout.amount really is grosze: 250000 -> 2500.00 PLN.
        assertEquals(Money.ofPln("2500.00"), payout.amount());
        assertEquals(9L, payout.id());
        assertEquals(PaymentOperator.PAYU, payout.operator());
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.type", equalTo("payout"))));
    }

    @Test
    void derivesTheNextCursorFromTheSortedFieldOfTheLastRow() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination[?(!@.after)]"))
                .willReturn(aResponse().withStatus(200).withBody(TWO_PAYMENTS_BODY)));
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination.after", equalTo("78")))
                .willReturn(aResponse().withStatus(200).withBody(ONE_PAYMENT_BODY)));

        List<Long> ids = client.payments()
                .searchPayments(PaymentSearch.builder()
                        .sortField(PaymentSortField.ID)
                        .order(SortOrder.DESCENDING)
                        .pageSize(2)
                        .build())
                .map(Payment::id)
                .toList();

        // Sorting by id means the cursor is the last id seen, sent as a JSON number.
        assertEquals(List.of(77L, 78L, 77L), ids);
        server.verify(2, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void writesAComparisonFilterAsFieldOperatorValue() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(EMPTY_BODY)));

        client.payments().searchPayments(PaymentSearch.builder()
                .matchingOrder(OrderId.of("202607x1234"))
                .build()).toList();

        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.filter.field", equalTo("orderId")))
                .withRequestBody(matchingJsonPath("$.filter.operator", equalTo("=")))
                .withRequestBody(matchingJsonPath("$.filter.value", equalTo("202607x1234"))));
    }

    @Test
    void writesAnInFilterAsAnArrayOfValues() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(EMPTY_BODY)));

        client.payments().searchPayouts(PayoutSearch.builder()
                .matchingIds(List.of(1L, 2L))
                .build()).toList();

        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.filter.operator", equalTo("in")))
                .withRequestBody(matchingJsonPath("$.filter.value[0]", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.filter.value[1]", equalTo("2"))));
    }

    @Test
    void sendsReturnSearchDatesAsWholeDaysAndPagesByNumber() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(EMPTY_BODY)));

        client.payments().searchReturns(ReturnSearch.builder(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 20))
                .market(Market.POLAND)
                .types(List.of("PAYOUT"))
                .pageSize(10)
                .build()).toList();

        // The spec types these as date-times, but the server accepts only yyyy-mm-dd.
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(equalToJson("""
                        {"type":"return","eventDateFrom":"2026-01-01","eventDateTo":"2026-07-20",
                         "page":1,"perPage":10,"types":["PAYOUT"],"market":"pl"}""")));
    }

    @Test
    void findPaymentSendsTypeAsAQueryParameterHere() {
        server.stubFor(get(urlPathEqualTo(OPERATION_PATH))
                .willReturn(aResponse().withStatus(200).withBody("""
                        {"id":1,"orderIds":[1234],"amount":149.99,"status":"COMPLETED",
                         "createdAt":"2026-07-24T10:00:00.000+02:00",
                         "completedAt":"2026-07-24T10:05:00.000+02:00",
                         "operator":"PAYU","methodCode":"PAYU.blik"}""")));

        Optional<Payment> payment = client.payments().findPayment(1L);

        assertTrue(payment.isPresent());
        // Unlike the search, this operation really does take type in the query.
        server.verify(getRequestedFor(urlEqualTo(OPERATION_PATH + "?type=payment")));
    }

    @Test
    void findPaymentReturnsEmptyRatherThanThrowingWhenAbsent() {
        // Real observed body, 2026-07-25: GET /payments/operations/1?type=payment on the sandbox.
        String body = """
                {"name":"NotFoundFailure","message":"Nie odnaleziono zasobu, Payment with id 1 for \
                shopId 100007 not found","failureType":"notFound",\
                "polishMessage":"Wystąpił nieoczekiwany błąd","spanId":"699t9Y-GUgTzF",\
                "traceId":"699t9Y-GUgTzF"}""";
        server.stubFor(get(urlPathEqualTo(OPERATION_PATH))
                .willReturn(aResponse().withStatus(404).withBody(body)));

        assertTrue(client.payments().findPayment(1L).isEmpty());
    }

    @Test
    void findPayoutAsksForThePayoutType() {
        server.stubFor(get(urlPathEqualTo(OPERATION_PATH))
                .willReturn(aResponse().withStatus(200).withBody("""
                        {"id":1,"amount":250000,"createdAt":"2026-07-20T08:00:00.000+02:00",
                         "operator":"PAYU"}""")));

        assertTrue(client.payments().findPayout(1L).isPresent());
        server.verify(getRequestedFor(urlEqualTo(OPERATION_PATH + "?type=payout")));
    }

    @Test
    void mapsADeepTransactionIncludingNestedOrdersAndRefund() {
        String body = """
                [{"type":"RETURN","status":"DONE","creationDate":"2026-07-01","eventDate":"2026-07-02",
                  "amount":12.5,"currency":"PLN","amountWithFee":13.0,"paymentId":77,"feeId":5,
                  "commissionFee":0.5,"fee":1.0,"providerId":"PRV-1",
                  "erliCreationDate":"2026-07-01T10:00:00.000+02:00",
                  "sortDate":"2026-07-01T10:00:00.000+02:00","payoutId":9,
                  "hasExternalOperationAssigned":true,"balanceSnapshot":{"available":100},
                  "customer":{"id":"C-1","name":"Jan"},
                  "refund":{"id":3,"orderId":"202607x1234","shop":"ext"},
                  "orders":[{"orderId":"202607x1234","subjectType":"order","deliveryPrice":9.99,
                             "items":[{"id":"I-1","quantity":2,"name":"Widget"}],"lockedFund":null}]}]""";
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(body)));

        Transaction transaction = client.payments()
                .searchReturns(ReturnSearch.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 20)))
                .findFirst().orElseThrow();

        assertEquals("RETURN", transaction.type().orElseThrow());
        assertEquals(Money.ofPln("12.5"), transaction.amount().orElseThrow());
        assertEquals("PLN", transaction.currency().orElseThrow());
        assertEquals(77L, transaction.paymentId().orElseThrow());
        assertEquals(9L, transaction.payoutId().orElseThrow());
        assertTrue(transaction.hasExternalOperationAssigned());
        assertEquals("C-1", transaction.customer().orElseThrow().id().orElseThrow());
        assertEquals(3L, transaction.refund().orElseThrow().id().orElseThrow());
        assertEquals(OrderId.of("202607x1234"), transaction.refund().orElseThrow().orderId().orElseThrow());
        assertEquals(1, transaction.orders().size());
        assertEquals(Money.ofPln("9.99"), transaction.orders().get(0).deliveryPrice().orElseThrow());
        assertEquals("Widget", transaction.orders().get(0).items().get(0).name().orElseThrow());
        assertTrue(transaction.balanceSnapshot().isPresent());
    }

    @Test
    void usesTheWireCurrencyForTransactionAmountsRatherThanAssumingZloty() {
        // Transaction lines are the one place the API states a currency per amount; stamping PLN on
        // a EUR line would make cross-market sums silently wrong.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody("""
                        [{"type":"RETURN","amount":12.5,"currency":"EUR","fee":1.0,
                          "orders":[{"orderId":"202607x1234","deliveryPrice":9.99}]}]""")));

        Transaction transaction = client.payments()
                .searchReturns(ReturnSearch.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 20)))
                .findFirst().orElseThrow();

        assertEquals("EUR", transaction.amount().orElseThrow().currency().getCurrencyCode());
        assertEquals("EUR", transaction.fee().orElseThrow().currency().getCurrencyCode());
        assertEquals("EUR",
                transaction.orders().get(0).deliveryPrice().orElseThrow().currency().getCurrencyCode());
    }

    @Test
    void fallsBackToZlotyWhenATransactionOmitsItsCurrency() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody("[{\"type\":\"RETURN\",\"amount\":12.5}]")));

        Transaction transaction = client.payments()
                .searchReturns(ReturnSearch.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 20)))
                .findFirst().orElseThrow();

        assertEquals("PLN", transaction.amount().orElseThrow().currency().getCurrencyCode());
    }

    @Test
    void keepsAPaymentWhoseMethodCodeThisSdkDoesNotRecognise() {
        // CORE-12: a PayU method added after this SDK's spec snapshot must not fail the whole read.
        // Everything else about the payment must still map.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody("""
                        [{"id":81,"orderIds":[1237],"amount":25.00,"status":"COMPLETED",
                          "createdAt":"2026-07-24T12:00:00.000+02:00",
                          "completedAt":"2026-07-24T12:01:00.000+02:00",
                          "operator":"PAYU","methodCode":"PAYU.somethingNewPayUAdded"}]""")));

        Payment payment = client.payments().searchPayments(PaymentSearch.all()).findFirst().orElseThrow();

        assertTrue(payment.methodCode().isEmpty(), "an unrecognised method decodes as absent");
        assertEquals(81L, payment.id());
        assertEquals(Money.ofPln("25.00"), payment.amount());
        assertEquals(PaymentStatus.COMPLETED, payment.status());
    }

    @Test
    void keepsAPayoutSettledByAnOperatorThisSdkDoesNotKnow() {
        // The operator change touched two call sites; the payout one resolves against a different
        // generated enum, so it is not covered by the payment test.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody("""
                        [{"id":11,"amount":250000,"createdAt":"2026-07-20T08:00:00.000+02:00",
                          "operator":"PRZELEWY24"}]""")));

        Payout payout = client.payments().searchPayouts(PayoutSearch.all()).findFirst().orElseThrow();

        assertEquals(PaymentOperator.UNRECOGNIZED, payout.operator());
        assertEquals(Money.ofPln("2500.00"), payout.amount(), "the rest of the payout still maps");
    }

    @Test
    void keepsAPaymentSettledByAnOperatorThisSdkDoesNotKnow() {
        // CORE-12 hardening: the operator list is Erli's to grow. One new provider must not fail the
        // read of every payment the shop has.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody("""
                        [{"id":83,"orderIds":[1239],"amount":30.00,"status":"COMPLETED",
                          "createdAt":"2026-07-24T12:00:00.000+02:00",
                          "completedAt":"2026-07-24T12:01:00.000+02:00",
                          "operator":"PRZELEWY24","methodCode":"PAYU.blik"}]""")));

        Payment payment = client.payments().searchPayments(PaymentSearch.all()).findFirst().orElseThrow();

        assertEquals(PaymentOperator.UNRECOGNIZED, payment.operator());
        assertEquals(Money.ofPln("30.00"), payment.amount(), "the rest of the payment still maps");
    }

    @Test
    void stillFailsLoudOnAnUnrecognisedPaymentStatus() {
        // The other half of CORE-12: a CLOSED enum stays fail-loud, so a status the SDK cannot model
        // is not silently swallowed into a wrong value.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody("""
                        [{"id":82,"orderIds":[1238],"amount":25.00,"status":"TELEPORTED",
                          "createdAt":"2026-07-24T12:00:00.000+02:00","operator":"PAYU",
                          "methodCode":"PAYU.blik"}]""")));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> client.payments().searchPayments(PaymentSearch.all()).toList());

        assertTrue(failure.getMessage().contains("status"), failure.getMessage());
    }

    @Test
    void mapsAPaymentThatHasNotCompletedYet() {
        // The spec marks completedAt required, but a PENDING payment cannot have one. Failing the
        // whole page on a missing value would be worse than modelling it as absent.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody("""
                        [{"id":80,"orderIds":[1236],"amount":10.00,"status":"PENDING",
                          "createdAt":"2026-07-24T11:00:00.000+02:00","operator":"PAYU",
                          "methodCode":"PAYU.blik"}]""")));

        Payment payment = client.payments().searchPayments(PaymentSearch.all()).findFirst().orElseThrow();

        assertEquals(PaymentStatus.PENDING, payment.status());
        assertTrue(payment.completedAt().isEmpty());
    }

    @Test
    void decodesAnEmptyResponseBodyWithoutFailing() {
        // A blank 200 body decodes to null with Class<T[]>; postList yields an empty list instead.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody("")));

        assertTrue(client.payments().searchPayments(PaymentSearch.all()).toList().isEmpty());
    }

    @Test
    void refusesToSilentlyNarrowAMultiValuedFilterOntoAScalarOperator() {
        // Sending only the first of several values would quietly return a different result set.
        assertThrows(IllegalArgumentException.class, () -> PaymentSearch.builder()
                .matchingAnyOf(PaymentSearch.PaymentFilterField.ID,
                        PaymentSearch.ComparisonOperator.EQUAL, List.of(1L, 2L))
                .build());
    }

    @Test
    void refusesAFilterWithNoValueRatherThanThrowingLaterFromTheStream() {
        assertThrows(IllegalArgumentException.class, () -> PaymentSearch.builder()
                .matchingAnyOf(PaymentSearch.PaymentFilterField.ID,
                        PaymentSearch.ComparisonOperator.IN, List.of())
                .build());
    }

    @Test
    void returnsPageByNumberAndStopOnAShortPage() {
        // Returns use a page counter, a third paging shape distinct from the two cursor paths, and it
        // was previously exercised only one page deep.
        String onePerPage = """
                [{"type":"RETURN","amount":1.0,"currency":"PLN"}]""";
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.page", equalTo("1")))
                .willReturn(aResponse().withStatus(200).withBody(onePerPage)));
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.page", equalTo("2")))
                .willReturn(aResponse().withStatus(200).withBody(onePerPage)));
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.page", equalTo("3")))
                .willReturn(aResponse().withStatus(200).withBody(EMPTY_BODY)));

        List<Transaction> all = client.payments().searchReturns(ReturnSearch.builder(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 20))
                .pageSize(1)
                .build()).toList();

        assertEquals(2, all.size(), "both full pages must be walked before the empty one stops it");
        server.verify(3, postRequestedFor(urlEqualTo(SEARCH_PATH)));
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.perPage", equalTo("1"))));
    }

    @Test
    void stopsReturnsWithoutASecondRequestWhenTheFirstPageIsShort() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(EMPTY_BODY)));

        assertTrue(client.payments().searchReturns(ReturnSearch.builder(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 20)).pageSize(10).build()).toList().isEmpty());

        server.verify(1, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void rejectsPayoutFilterPairingsTheApiWouldReject() {
        // The API's payout filter has two mutually exclusive shapes: id only with in/nin, and
        // createdAt/amount only with the ordering operators.
        assertThrows(IllegalArgumentException.class, () -> PayoutSearch.builder()
                .matching(PayoutSearch.PayoutFilterField.ID, PaymentSearch.ComparisonOperator.EQUAL, 5L)
                .build());
        assertThrows(IllegalArgumentException.class, () -> PayoutSearch.builder()
                .matching(PayoutSearch.PayoutFilterField.AMOUNT, PaymentSearch.ComparisonOperator.IN, 5L)
                .build());
    }

    @Test
    void searchesFetchLazily() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(200).withBody(TWO_PAYMENTS_BODY)));

        List<Payment> first = client.payments()
                .searchPayments(PaymentSearch.builder().pageSize(2).build())
                .limit(1)
                .toList();

        assertEquals(1, first.size());
        server.verify(1, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }
}
