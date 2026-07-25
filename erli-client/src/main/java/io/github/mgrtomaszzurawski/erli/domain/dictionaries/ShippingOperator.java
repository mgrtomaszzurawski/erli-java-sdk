package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The logistics operator behind an ERLI {@link ShippingMethod}. Values mirror the Erli dictionary;
 * the SDK maps by the wire string (via {@link #fromWire}), not by enum name, so the domain constant
 * names stay decoupled from the generated Layer-1 ones.
 */
public enum ShippingOperator {

    INPOST("INPOST"),
    DHL("DHL"),
    DPD("DPD"),
    POCZTA("POCZTA"),
    RUCH("RUCH");

    private static final Map<String, ShippingOperator> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ShippingOperator::wireValue, Function.identity()));

    private final String wireValue;

    ShippingOperator(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this operator is sent as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to an operator.
     *
     * <p>Only ever called with the wire value of an already-decoded Layer-1 {@code OperatorEnum}, so
     * an operator the API added but the vendored spec lacks fails earlier, at JSON decode (fail-loud,
     * see {@code KNOWN-SERVER-BEHAVIORS.md}). This guard fires only on domain/generated enum drift.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static ShippingOperator fromWire(String wireValue) {
        ShippingOperator operator = BY_WIRE.get(wireValue);
        if (operator == null) {
            throw new ErliTransportException(
                    "No ShippingOperator constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return operator;
    }
}
