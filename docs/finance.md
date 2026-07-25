# Finance — payments, payouts, billing, commissions and campaigns

Everything about seller money: what buyers paid, what Erli paid out, what it charged, what a
listing would cost, and what advertising spent.

All amounts are exposed as `Money`. The API sends most of them as whole *grosze* and states no
currency, so they are PLN — the Erli marketplace settles in złoty; the SDK converts, so you never
divide by 100. The one exception is operator transaction lines, which carry their own
[`currency()`](#returns-the-operators-transaction-history).

```java
try (ErliClient client = ErliClient.fromEnvironment()) {
    // ...
}
```

## Commissions — what a listing will cost

Estimate today's marketplace commission before creating a listing. The category must be a **leaf**
category; a non-leaf id is rejected with `ErliValidationException`.

```java
CommissionEstimate estimate = client.commissions().estimate(
        CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of("4"))
                .unitPrice(Money.ofPln("100.00"))
                .quantity(3)
                .build());

estimate.commission();   // 35.04 PLN
```

It is an estimate for the current day, not a quote — rates can change.

## Billing — the company settlement ledger

Both operations cover the **whole company** (every shop it owns); narrow with `shopId(...)`.
Results are newest-first and lazy: pages are fetched only as the stream is consumed.

```java
// The five most recent entries — one request, not the whole ledger.
List<BillingEntry> recent = client.billing()
        .entries(BillingEntryFilter.all())
        .limit(5)
        .toList();

// Everything charged against one order in July.
client.billing().entries(BillingEntryFilter.builder()
                .orderId(OrderId.of("202607x1234"))
                .fromOccurredAt(OffsetDateTime.parse("2026-07-01T00:00:00+02:00"))
                .build())
        .forEach(entry -> System.out.println(entry.type() + " " + entry.amount()));
```

`amount()` is signed — negative for what Erli charged, positive for what it credited — and
`balanceAfter()` is the running balance, so a statement reconciles without re-summing.

`rebates(...)` returns the same entry shape restricted to rebates, where `rebateOrigin()` breaks the
total down by reason.

Entry types come from `GET /dictionaries/billingEntryTypes` (the Dictionaries bucket).

## Payments — money in, money out

One endpoint serves three kinds of operation; the SDK splits them into typed methods so nothing is
downcast.

```java
// Payments that settled for one order.
client.payments().searchPayments(PaymentSearch.builder()
                .matchingOrder(OrderId.of("202607x1234"))
                .build())
        .filter(payment -> payment.status() == PaymentStatus.COMPLETED)
        .forEach(payment -> System.out.println(
                payment.amount() + " via " + payment.methodCode().orElse("unknown method")));

// Largest payouts first.
client.payments().searchPayouts(PayoutSearch.builder()
                .sortField(PayoutSortField.AMOUNT)
                .order(SortOrder.DESCENDING)
                .build())
        .limit(10)
        .toList();

// A single operation by id — empty rather than an exception when it does not exist.
Optional<Payment> payment = client.payments().findPayment(77L);
```

`Payment`, `Payout` and `Transaction` share the sealed `PaymentOperation` type, so results of the
three searches can be combined into one timeline and dispatched over a closed set of cases. On the
SDK's Java 17 baseline use `instanceof` patterns; on Java 21+ the same dispatch can be an exhaustive
`switch` with no default branch.

### Returns (the operator's transaction history)

Broader than payments and payouts — it also covers fees, chargebacks and transfers. It needs a
bounded event-date range, and it pages by number rather than by cursor.

```java
client.payments().searchReturns(ReturnSearch.builder(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))
        .types(List.of("PAYOUT", "CHARGEBACK"))
        .market(Market.PL)
        .build())
        .forEach(transaction -> System.out.println(transaction.type() + " " + transaction.amount()));
```

Backed by the payment operator, so it can report not-found when the shop has no operator account.

Note that `Transaction` amounts carry their own `currency()` and are in major units, unlike the
grosze used elsewhere in this domain.

## Campaigns — advertising spend

Both dates are **required** and `endDate` must not be in the future.

```java
CampaignCostSummary summary = client.campaigns()
        .costSummary(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 20));

summary.totalNetCost();   // whole-range net spend
summary.dailyCosts();     // one row per campaign per day
```

Costs are net (excluding VAT). A row without a `campaignId()` is spend the API did not attribute to
a single campaign.

## Values this SDK does not recognise

Erli grows some of these lists without a spec release, so the SDK draws a line between the ones it
can tolerate and the ones it must not:

| Field | If Erli sends something new | Why |
|---|---|---|
| `Payment.methodCode` / `methodName` | empty `Optional` | ~44 operator method codes that change often; the rest of the payment still maps |
| `Payment.operator` / `Payout.operator` | `PaymentOperator.UNRECOGNIZED` | the provider list is Erli's to grow; a new one must not break every payment read |
| `TransactionOrder.subjectType` | empty `Optional` | same reasoning |
| `Payment.status` | **throws** `IllegalStateException` | a closed lifecycle you branch on to decide whether money arrived — a state the SDK cannot model is refused rather than mapped to something that reads as "not settled" |

```java
// Safe against a method PayU adds tomorrow.
String method = payment.methodCode().orElse("unknown");

if (payment.operator() == PaymentOperator.UNRECOGNIZED) {
    // Settled by a provider this SDK version does not know. Amounts and dates are still correct.
}
```

One caveat worth knowing: the raw wire value is gone by the time the SDK maps it, so an unrecognised
value and an absent one are indistinguishable. If you need the literal string Erli sent, you need a
newer SDK release with the updated spec.

## Errors

Every operation maps failures to the remediation exceptions from `sdk.core`:

| HTTP | Exception | Typical cause here |
|---|---|---|
| 401 / 403 | `ErliAuthException` | key missing, wrong or lacking scope |
| 404 | `ErliNotFoundException` | unknown operation id (`find*` returns empty instead) |
| 400 / 409 / 422 | `ErliValidationException` | non-leaf category, missing or future date, bad filter |
| 5xx | `ErliServerException` | upstream failure; retried first |

All carry `traceId`/`spanId` and Erli's `polishMessage` for support tickets.

> **Rate limiting reads as a validation error today.** Erli rate-limits, and a 429 currently surfaces
> as `ErliValidationException` because the SDK maps every non-auth, non-not-found 4xx that way. The
> remediation is the opposite of what that name suggests — back off and retry, do not change the
> request. Check `details().httpStatus() == 429` to tell them apart. Tracked as CORE-14.

Bad input is rejected before the wire: a non-PLN or sub-grosz amount, a page size above the API cap,
or an inverted date range all raise `IllegalArgumentException`.
