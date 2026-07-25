package io.github.mgrtomaszzurawski.erli.domain.payments;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * One line of an order covered by a transaction.
 *
 * @param id       the item's identifier
 * @param name     the item's name at the time of sale
 * @param quantity how many units; the API types this as a number, not an integer
 */
public record TransactionOrderItem(Optional<String> id, Optional<String> name, Optional<BigDecimal> quantity) {
}
