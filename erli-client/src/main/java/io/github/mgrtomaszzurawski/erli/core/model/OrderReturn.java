package io.github.mgrtomaszzurawski.erli.core.model;

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

    /**
     * A log-safe rendering. The generated {@code toString} would print {@link #comment()} — buyer-authored
     * free text, exactly where a buyer writes a phone number — so it is omitted here by name; read the
     * accessor when you need it. {@link #bankAccount()} is omitted for the same reason.
     */
    @Override
    public String toString() {
        return "OrderReturn[reason=" + reason
                + ", lines=" + lines.size()
                + ", created=" + created
                + "]";
    }
}
