package io.github.mgrtomaszzurawski.erli.internal.client.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.PaymentStatus;
import io.github.mgrtomaszzurawski.erli.domain.payments.Payment;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentOperator;
import io.github.mgrtomaszzurawski.erli.domain.payments.Payout;
import io.github.mgrtomaszzurawski.erli.domain.payments.Transaction;
import io.github.mgrtomaszzurawski.erli.domain.payments.TransactionCustomer;
import io.github.mgrtomaszzurawski.erli.domain.payments.TransactionOrder;
import io.github.mgrtomaszzurawski.erli.domain.payments.TransactionOrderItem;
import io.github.mgrtomaszzurawski.erli.domain.payments.TransactionRefund;
import io.github.mgrtomaszzurawski.erli.domain.payments.TransactionSubjectType;
import io.github.mgrtomaszzurawski.erli.internal.client.finance.MinorUnits;
import io.github.mgrtomaszzurawski.erli.rest.model.TransactionOrdersInner;
import io.github.mgrtomaszzurawski.erli.rest.model.TransactionOrdersInnerItemsInner;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated payment, payout and transaction models to their domain records. Internal.
 */
final class PaymentMapper {

    private static final String FIELD_ID = "id";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_ORDER_IDS = "orderIds";
    private static final String OPERATOR_PAYU_WIRE_VALUE = "PAYU";
    private static final String RAW_PAYMENT_NAME = "raw Payment";
    private static final String RAW_PAYOUT_NAME = "raw Payout";
    private static final String RAW_TRANSACTION_NAME = "raw Transaction";

    private PaymentMapper() {
    }

    static Payment toPayment(io.github.mgrtomaszzurawski.erli.rest.model.Payment rawPayment) {
        Objects.requireNonNull(rawPayment, RAW_PAYMENT_NAME);
        return new Payment(
                require(rawPayment.getId(), FIELD_ID).longValue(),
                toOrderIds(rawPayment.getOrderIds()),
                // Payment.amount is the one Erli money field expressed in złoty, not grosze.
                MinorUnits.fromMajorUnits(require(rawPayment.getAmount(), FIELD_AMOUNT)),
                toStatus(require(rawPayment.getStatus(), FIELD_STATUS)),
                require(rawPayment.getCreatedAt(), FIELD_CREATED_AT),
                Optional.ofNullable(rawPayment.getCompletedAt()),
                toOperator(rawPayment.getOperator()),
                // CORE-12: an unrecognised operator method decodes to null rather than throwing, and
                // this enum grows without a spec release — so absence here is expected, not an error.
                Optional.ofNullable(rawPayment.getMethodCode())
                        .map(io.github.mgrtomaszzurawski.erli.rest.model.Payment.MethodCodeEnum::getValue),
                Optional.ofNullable(rawPayment.getMethodName())
                        .map(io.github.mgrtomaszzurawski.erli.rest.model.Payment.MethodNameEnum::getValue),
                Optional.ofNullable(rawPayment.getExternalPaymentId()));
    }

    static Payout toPayout(io.github.mgrtomaszzurawski.erli.rest.model.Payout rawPayout) {
        Objects.requireNonNull(rawPayout, RAW_PAYOUT_NAME);
        return new Payout(
                require(rawPayout.getId(), FIELD_ID).longValue(),
                MinorUnits.fromGrosze(require(rawPayout.getAmount(), FIELD_AMOUNT)),
                require(rawPayout.getCreatedAt(), FIELD_CREATED_AT),
                toOperator(rawPayout.getOperator()));
    }

    static Transaction toTransaction(io.github.mgrtomaszzurawski.erli.rest.model.Transaction rawTransaction) {
        Objects.requireNonNull(rawTransaction, RAW_TRANSACTION_NAME);
        // Transaction lines state their own currency; everything else in Finance is PLN grosze.
        String currencyCode = rawTransaction.getCurrency();
        return new Transaction(
                Optional.ofNullable(rawTransaction.getType()),
                Optional.ofNullable(rawTransaction.getStatus()),
                Optional.ofNullable(rawTransaction.getCreationDate()),
                Optional.ofNullable(rawTransaction.getEventDate()),
                Optional.ofNullable(rawTransaction.getErliCreationDate()),
                Optional.ofNullable(rawTransaction.getSortDate()),
                money(rawTransaction.getAmount(), currencyCode),
                money(rawTransaction.getAmountWithFee(), currencyCode),
                money(rawTransaction.getFee(), currencyCode),
                money(rawTransaction.getCommissionFee(), currencyCode),
                Optional.ofNullable(rawTransaction.getCurrency()),
                Optional.ofNullable(rawTransaction.getPaymentId()).map(BigDecimal::longValue),
                Optional.ofNullable(rawTransaction.getPayoutId()).map(BigDecimal::longValue),
                Optional.ofNullable(rawTransaction.getFeeId()).map(BigDecimal::longValue),
                Optional.ofNullable(rawTransaction.getProviderId()),
                toTransactionOrders(rawTransaction.getOrders(), currencyCode),
                Optional.ofNullable(rawTransaction.getRefund()).map(PaymentMapper::toRefund),
                Optional.ofNullable(rawTransaction.getCustomer()).map(PaymentMapper::toCustomer),
                Optional.ofNullable(rawTransaction.getBalanceSnapshot()),
                Boolean.TRUE.equals(rawTransaction.getHasExternalOperationAssigned()));
    }

    private static List<TransactionOrder> toTransactionOrders(
            List<TransactionOrdersInner> rawOrders, String currencyCode) {
        if (rawOrders == null) {
            return List.of();
        }
        return rawOrders.stream().map(rawOrder -> toTransactionOrder(rawOrder, currencyCode)).toList();
    }

    private static TransactionOrder toTransactionOrder(TransactionOrdersInner rawOrder, String currencyCode) {
        return new TransactionOrder(
                Optional.ofNullable(rawOrder.getOrderId()).map(OrderId::of),
                Optional.ofNullable(rawOrder.getSubjectType()).map(PaymentMapper::toSubjectType),
                money(rawOrder.getDeliveryPrice(), currencyCode),
                toOrderItems(rawOrder.getItems()),
                Optional.ofNullable(rawOrder.getShop()),
                Optional.ofNullable(rawOrder.getLockedFund()));
    }

    private static List<TransactionOrderItem> toOrderItems(List<TransactionOrdersInnerItemsInner> rawItems) {
        if (rawItems == null) {
            return List.of();
        }
        return rawItems.stream()
                .map(rawItem -> new TransactionOrderItem(
                        Optional.ofNullable(rawItem.getId()),
                        Optional.ofNullable(rawItem.getName()),
                        Optional.ofNullable(rawItem.getQuantity())))
                .toList();
    }

    private static TransactionRefund toRefund(
            io.github.mgrtomaszzurawski.erli.rest.model.TransactionRefund rawRefund) {
        return new TransactionRefund(
                Optional.ofNullable(rawRefund.getId()).map(BigDecimal::longValue),
                Optional.ofNullable(rawRefund.getOrderId()).map(OrderId::of),
                Optional.ofNullable(rawRefund.getShop()));
    }

    private static TransactionCustomer toCustomer(
            io.github.mgrtomaszzurawski.erli.rest.model.TransactionCustomer rawCustomer) {
        return new TransactionCustomer(
                Optional.ofNullable(rawCustomer.getId()),
                Optional.ofNullable(rawCustomer.getName()));
    }

    /**
     * Transaction amounts arrive in major units and carry their own currency, unlike the PLN grosze
     * used by the rest of the Finance domain.
     */
    private static Optional<Money> money(BigDecimal majorUnits, String currencyCode) {
        return Optional.ofNullable(majorUnits).map(amount -> MinorUnits.fromMajorUnits(amount, currencyCode));
    }

    private static List<OrderId> toOrderIds(List<Integer> rawOrderIds) {
        List<Integer> orderIds = require(rawOrderIds, FIELD_ORDER_IDS);
        // The spec types these as integers here but as the "NNNNNNxNNNN" text form everywhere else;
        // core owns OrderId, so keep one type and carry whatever the server sent as its text.
        return orderIds.stream().map(String::valueOf).map(OrderId::of).toList();
    }

    private static PaymentStatus toStatus(
            io.github.mgrtomaszzurawski.erli.rest.model.Payment.StatusEnum status) {
        return switch (status) {
            case NEW -> PaymentStatus.NEW;
            case PENDING -> PaymentStatus.PENDING;
            case WAITING_FOR_CONFIRMATION -> PaymentStatus.WAITING_FOR_CONFIRMATION;
            case COMPLETED -> PaymentStatus.COMPLETED;
            case CANCELED -> PaymentStatus.CANCELED;
        };
    }

    private static TransactionSubjectType toSubjectType(TransactionOrdersInner.SubjectTypeEnum subjectType) {
        return switch (subjectType) {
            case ORDER -> TransactionSubjectType.ORDER;
            case ADS_PAY_IN -> TransactionSubjectType.ADS_PAY_IN;
            case VINDICATION_CASE -> TransactionSubjectType.VINDICATION_CASE;
            case RETURN_PARCELS -> TransactionSubjectType.RETURN_PARCELS;
            case FREE_RETURN -> TransactionSubjectType.FREE_RETURN;
            case DEPOSIT_FUND -> TransactionSubjectType.DEPOSIT_FUND;
        };
    }

    /**
     * Map the payment's operator, tolerating one Erli adds later. CORE-12 decodes an unknown enum
     * value to null, so null here means "a provider this SDK does not know" — a growing enum, so it
     * takes the sentinel rather than failing the whole page. Contrast {@link #toStatus}, a closed
     * lifecycle that stays fail-loud.
     *
     * <p>Typed to the generated enum rather than {@code <T extends Enum<T>>}: a wildcard signature
     * would accept any enum in the model, so passing the wrong field would silently yield a sentinel
     * instead of failing to compile.
     */
    private static PaymentOperator toOperator(
            io.github.mgrtomaszzurawski.erli.rest.model.Payment.OperatorEnum operator) {
        return operator == null ? PaymentOperator.UNRECOGNIZED : fromWireValue(operator.getValue());
    }

    /** Same for the payout's operator; the generator emits a separate enum per schema. */
    private static PaymentOperator toOperator(
            io.github.mgrtomaszzurawski.erli.rest.model.Payout.OperatorEnum operator) {
        return operator == null ? PaymentOperator.UNRECOGNIZED : fromWireValue(operator.getValue());
    }

    /** Match on the value Erli sends, not the generated constant name — the generator sanitizes names. */
    private static PaymentOperator fromWireValue(String wireValue) {
        return OPERATOR_PAYU_WIRE_VALUE.equalsIgnoreCase(wireValue)
                ? PaymentOperator.PAYU
                : PaymentOperator.UNRECOGNIZED;
    }

    /**
     * Require a field the SDK cannot do without. Since CORE-12, an unrecognised <em>enum</em> value
     * also arrives as null, so the message names both possibilities rather than sending the reader
     * looking for a field that was in fact present.
     */
    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Payment operation is missing the required '" + fieldName
                    + "' field, or carries a value this SDK does not recognise");
        }
        return value;
    }
}
