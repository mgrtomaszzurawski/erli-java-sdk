package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One return registered against the order.
 *
 * @param lines       the returned positions (the API documents at least one; not enforced here,
 *                    because a response is reported as it arrives)
 * @param bankAccount where to pay the refund, when the buyer supplied an account
 * @param reason      why the buyer returned the items
 * @param comment     the buyer's own words, when they left any
 * @param created     when the return was registered
 */
public record OrderReturn(
        List<ReturnedLine> lines,
        Optional<BankAccount> bankAccount,
        ReturnReason reason,
        Optional<String> comment,
        OffsetDateTime created) {

    public OrderReturn {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(bankAccount, "bankAccount");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(created, "created");
        lines = List.copyOf(lines);
    }
}
