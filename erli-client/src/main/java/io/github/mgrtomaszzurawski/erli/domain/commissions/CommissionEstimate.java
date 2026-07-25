package io.github.mgrtomaszzurawski.erli.domain.commissions;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

/**
 * The marketplace commission Erli would charge today for the requested listing, as returned by
 * {@code POST /commissions/_estimate}. It is an estimate for the current day only — commission rates
 * can change, so it is not a quote.
 *
 * @param commission the estimated commission for the whole requested quantity, never negative
 */
public record CommissionEstimate(Money commission) {
}
