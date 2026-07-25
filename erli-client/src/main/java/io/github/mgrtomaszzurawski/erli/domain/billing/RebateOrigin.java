package io.github.mgrtomaszzurawski.erli.domain.billing;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

/**
 * One component of a rebate: how much was granted and why. A rebate entry can be made up of several
 * of these.
 *
 * @param amount       the amount attributed to this reason
 * @param rebateReason Erli's code or description for why the rebate was granted
 */
public record RebateOrigin(Money amount, String rebateReason) {
}
