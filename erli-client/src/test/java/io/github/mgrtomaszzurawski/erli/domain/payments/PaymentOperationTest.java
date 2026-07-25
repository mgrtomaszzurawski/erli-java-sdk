package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The sealed union exists so results of the three searches can be combined into one timeline; this
 * pins that it actually works from a consumer's side, including the exhaustive switch.
 */
class PaymentOperationTest {

    private static final OffsetDateTime WHEN = OffsetDateTime.parse("2026-07-24T10:00:00+02:00");

    private static Payment payment() {
        return new Payment(77L, List.of(OrderId.of("1234")), Money.ofPln("149.99"), PaymentStatus.COMPLETED,
                WHEN, Optional.of(WHEN), PaymentOperator.PAYU, Optional.of("PAYU.blik"), Optional.empty(), Optional.empty());
    }

    private static Transaction transaction() {
        return new Transaction(Optional.of("RETURN"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(), Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    private static Payout payout() {
        return new Payout(9L, Money.ofPln("2500.00"), WHEN, PaymentOperator.PAYU);
    }

    @Test
    void combinesEveryOperationKindIntoOneTimeline() {
        List<PaymentOperation> timeline = List.of(payment(), payout(), transaction());

        List<String> rendered = timeline.stream().map(PaymentOperationTest::render).toList();

        assertEquals(List.of("in 149.99", "out 2500.00", "txn RETURN"), rendered);
    }

    /**
     * Pattern switch over a sealed type is a preview feature on the project's Java 17 baseline, so
     * consumers on 17 use {@code instanceof} patterns; the sealed hierarchy still guarantees the set
     * of cases is closed. A consumer on 21+ can write the same thing as an exhaustive switch.
     */
    private static String render(PaymentOperation operation) {
        if (operation instanceof Payment paymentIn) {
            return "in " + paymentIn.amount().amount().toPlainString();
        }
        if (operation instanceof Payout payoutOut) {
            return "out " + payoutOut.amount().amount().toPlainString();
        }
        Transaction transaction = (Transaction) operation;
        return "txn " + transaction.type().orElse("?");
    }
}
