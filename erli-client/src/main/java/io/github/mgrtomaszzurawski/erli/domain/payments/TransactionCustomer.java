package io.github.mgrtomaszzurawski.erli.domain.payments;

import java.util.Optional;

/**
 * The counterparty on a transaction, as the payment operator identifies them.
 *
 * @param id   the operator's identifier for the customer
 * @param name the customer's name
 */
public record TransactionCustomer(Optional<String> id, Optional<String> name) {
}
