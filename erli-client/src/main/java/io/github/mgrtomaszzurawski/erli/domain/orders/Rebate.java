package io.github.mgrtomaszzurawski.erli.domain.orders;

import java.util.Optional;

/**
 * A discount applied to the whole order. When one is present, the affected {@link OrderItem}s also
 * carry {@link OrderItem#unitPriceBeforeRebate()}.
 *
 * @param id   Erli's numeric id of the rebate
 * @param name the rebate's display name
 * @param code the redeemed code — present only for single-use codes
 */
public record Rebate(long id, String name, Optional<String> code) {
}
