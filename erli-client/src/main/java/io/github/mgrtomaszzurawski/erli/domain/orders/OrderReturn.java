package io.github.mgrtomaszzurawski.erli.domain.orders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * A return the buyer opened against the order. An order can accumulate several — a buyer may send
 * back different lines at different times — so {@link Order#returns()} is a list.
 *
 * @param items       the returned lines; never empty
 * @param bankAccount where the refund is to be paid, when the buyer supplied an account
 * @param reason      why the buyer returned the goods
 * @param comment     the buyer's own words, when they added any
 * @param created     when the return was opened
 */
public record OrderReturn(
        List<ReturnedItem> items,
        Optional<BankAccount> bankAccount,
        ReturnReason reason,
        Optional<String> comment,
        OffsetDateTime created) {

    /** Defensively copies {@code items} so the record is genuinely immutable. */
    public OrderReturn {
        items = List.copyOf(items);
    }
}
