package io.github.mgrtomaszzurawski.erli.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The discount applied to the order, if any.
 *
 * @param id   the marketplace's id for the rebate
 * @param name the rebate's name
 * @param code the redeemed code — present only for single-use codes
 */
public record Rebate(long id, String name, Optional<String> code) {

    public Rebate {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(code, "code");
    }
}
