package io.github.mgrtomaszzurawski.erli.domain.billing;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * One line in the company's settlement ledger with Erli — a commission charge, a payout, a rebate,
 * and so on.
 *
 * <p>{@link #amount()} is signed: negative for what Erli charged the company, positive for what it
 * credited. {@link #balanceAfter()} is the running balance once this entry was applied, so a
 * statement can be reconciled without re-summing the ledger.
 *
 * @param id           the entry's identifier, also the pagination cursor
 * @param occurredAt   when the entry was booked
 * @param type         the entry type, as listed by {@code GET /dictionaries/billingEntryTypes}
 * @param description  the human-readable description Erli attached
 * @param amount       the signed amount of this entry
 * @param balanceAfter the account balance immediately after this entry
 * @param orderId      the order this entry relates to, when it relates to one
 * @param shopId       the shop this entry belongs to, when attributed to one
 * @param productId    the product this entry relates to, when it relates to one
 * @param rebateOrigin the rebates making up this entry; empty unless the entry is a rebate
 */
public record BillingEntry(
        long id,
        OffsetDateTime occurredAt,
        String type,
        String description,
        Money amount,
        Money balanceAfter,
        Optional<OrderId> orderId,
        Optional<Long> shopId,
        Optional<Long> productId,
        List<RebateOrigin> rebateOrigin) {

    public BillingEntry {
        rebateOrigin = List.copyOf(rebateOrigin);
    }
}
